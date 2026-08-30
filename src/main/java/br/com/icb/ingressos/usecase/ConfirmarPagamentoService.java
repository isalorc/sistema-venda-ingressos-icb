package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.NotificacaoPort;
import br.com.icb.ingressos.ports.out.NotificacaoPort.IngressoEmitido;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

/**
 * Processa a notificação de resultado de pagamento vinda do webhook (RF-11 a RF-14).
 *
 * <p><strong>Idempotente</strong> (RF-12, RNF-09): a notificação é correlacionada
 * ao {@code Pagamento} pela referência da cobrança; se o pagamento já saiu de
 * {@code PENDENTE}, a chamada é ignorada. Assim uma reentrega (ou entrega fora de
 * ordem) não gera efeito duplicado.
 */
@Service
public class ConfirmarPagamentoService implements ConfirmarPagamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmarPagamentoService.class);

    private final PagamentoRepositoryPort pagamentoRepository;
    private final PedidoRepositoryPort pedidoRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final LoteRepositoryPort loteRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final EventoRepositoryPort eventoRepository;
    private final NotificacaoPort notificacao;
    private final Clock clock;

    public ConfirmarPagamentoService(PagamentoRepositoryPort pagamentoRepository,
                                     PedidoRepositoryPort pedidoRepository,
                                     IngressoRepositoryPort ingressoRepository,
                                     LoteRepositoryPort loteRepository,
                                     UsuarioRepositoryPort usuarioRepository,
                                     EventoRepositoryPort eventoRepository,
                                     NotificacaoPort notificacao,
                                     Clock clock) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.ingressoRepository = ingressoRepository;
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.notificacao = notificacao;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void confirmar(NotificacaoPagamento notificacaoPagamento) {
        var pagamento = pagamentoRepository.buscarPorReferenciaGateway(notificacaoPagamento.referenciaGateway())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pagamento pela referência do gateway", notificacaoPagamento.referenciaGateway()));

        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            log.info("Notificação ignorada: pagamento {} já está {} (referência {}).",
                    pagamento.getId(), pagamento.getStatus(), pagamento.getReferenciaGateway());
            return;
        }

        var pedido = pedidoRepository.buscarPorId(pagamento.getPedidoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pagamento.getPedidoId()));
        var ingressos = ingressoRepository.listarPorPedido(pedido.getId());

        switch (notificacaoPagamento.resultado()) {
            case APROVADO -> aprovar(pagamento, pedido, ingressos);
            case RECUSADO -> recusar(pagamento, pedido, ingressos);
        }
    }

    private void aprovar(Pagamento pagamento, Pedido pedido, List<Ingresso> ingressos) {
        pagamento.aprovar(LocalDateTime.now(clock));
        pedido.marcarPago();

        for (var ingresso : ingressos) {
            ingresso.confirmarVenda(gerarCodigoQr());
            ingressoRepository.salvar(ingresso);
        }
        pagamentoRepository.salvar(pagamento);
        pedidoRepository.salvar(pedido);

        notificarCliente(pedido, ingressos);
    }

    private void recusar(Pagamento pagamento, Pedido pedido, List<Ingresso> ingressos) {
        for (var ingresso : ingressos) {
            ingresso.liberarReserva();
            ingressoRepository.salvar(ingresso);

            var lote = loteRepository.buscarPorId(ingresso.getLoteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Lote", ingresso.getLoteId()));
            lote.liberarUnidade();
            loteRepository.salvar(lote);
        }
        pagamento.recusar();
        pedido.cancelar();
        pagamentoRepository.salvar(pagamento);
        pedidoRepository.salvar(pedido);
    }

    /** RN-4: entrega do ingresso por e-mail. Enquanto o template não é definido, a porta é um stub. */
    private void notificarCliente(Pedido pedido, List<Ingresso> ingressos) {
        if (ingressos.isEmpty()) {
            return;
        }
        var cliente = usuarioRepository.buscarPorId(pedido.getUsuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", pedido.getUsuarioId()));
        for (var ingresso : ingressos) {
            var evento = eventoRepository.buscarPorId(ingresso.getEventoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", ingresso.getEventoId()));
            notificacao.enviarIngresso(new IngressoEmitido(
                    cliente.getNome(), cliente.getEmail(),
                    evento.getNome(), evento.getDataHora(), ingresso.getCodigoQr()));
        }
    }

    /** RN-6: código QR gerado só aqui, após a aprovação do pagamento. */
    private String gerarCodigoQr() {
        return UUID.randomUUID().toString();
    }
}
