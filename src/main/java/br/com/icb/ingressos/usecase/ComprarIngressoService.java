package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort.DadosCobranca;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

/**
 * Orquestra a compra de um ingresso (RF-04 a RF-10).
 *
 * <p>A operação é transacional e serializa a reserva pelo bloqueio pessimista do
 * lote ({@link LoteRepositoryPort#buscarPorIdComBloqueio} — RNF-07): duas compras
 * concorrentes do mesmo lote esperam a transação anterior, o que impede a
 * quantidade disponível de ficar negativa e o mesmo ingresso de ser reservado
 * duas vezes.
 */
@Service
public class ComprarIngressoService implements ComprarIngressoUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final LoteRepositoryPort loteRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final PedidoRepositoryPort pedidoRepository;
    private final PagamentoRepositoryPort pagamentoRepository;
    private final GatewayPagamentoPort gatewayPagamento;
    private final Clock clock;

    public ComprarIngressoService(UsuarioRepositoryPort usuarioRepository,
                                  LoteRepositoryPort loteRepository,
                                  IngressoRepositoryPort ingressoRepository,
                                  PedidoRepositoryPort pedidoRepository,
                                  PagamentoRepositoryPort pagamentoRepository,
                                  GatewayPagamentoPort gatewayPagamento,
                                  Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
        this.ingressoRepository = ingressoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.gatewayPagamento = gatewayPagamento;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ResultadoCompra comprar(ComprarIngressoCommand comando) {
        var cliente = resolverCliente(comando);

        var lote = loteRepository.buscarPorIdComBloqueio(comando.loteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lote", comando.loteId()));

        var ingresso = ingressoRepository.buscarPrimeiroDisponivelDoLote(lote.getId())
                .orElseThrow(() -> new IngressoEsgotadoException(lote.getId()));

        lote.reservarUnidade();

        var pedido = pedidoRepository.salvar(
                Pedido.novo(cliente.getId(), agora(), lote.getPreco()));

        ingresso.reservar(pedido.getId());
        var ingressoReservado = ingressoRepository.salvar(ingresso);
        loteRepository.salvar(lote);

        var cobranca = solicitarCobranca(comando, pedido, lote, cliente);

        return new ResultadoCompra(
                pedido.getId(),
                ingressoReservado.getId(),
                pedido.getValorTotal(),
                pedido.getStatus(),
                cobranca.urlPagamento(),
                cobranca.qrCodePix());
    }

    /** RN-4: sem cadastro — reaproveita o cliente pelo e-mail ou cria um novo. */
    private Usuario resolverCliente(ComprarIngressoCommand comando) {
        if (comando.emailCliente() != null && !comando.emailCliente().isBlank()) {
            var email = comando.emailCliente().strip().toLowerCase(Locale.ROOT);
            var existente = usuarioRepository.buscarPorEmail(email);
            if (existente.isPresent()) {
                return existente.get();
            }
        }
        return usuarioRepository.salvar(Usuario.novo(
                comando.nomeCliente(), comando.emailCliente(), comando.telefoneCliente(), agora()));
    }

    private GatewayPagamentoPort.CobrancaCriada solicitarCobranca(ComprarIngressoCommand comando,
                                                                  Pedido pedido, Lote lote, Usuario cliente) {
        var pagamento = pagamentoRepository.salvar(
                Pagamento.pendente(pedido.getId(), lote.getPreco(), comando.metodoPagamento()));

        var cobranca = gatewayPagamento.criarCobranca(new DadosCobranca(
                pedido.getId(), lote.getPreco(), comando.metodoPagamento(),
                cliente.getNome(), cliente.getEmail()));

        pagamento.vincularCobranca(cobranca.referenciaGateway());
        pagamentoRepository.salvar(pagamento);
        return cobranca;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
