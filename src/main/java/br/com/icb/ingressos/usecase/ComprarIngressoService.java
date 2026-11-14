package br.com.icb.ingressos.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.ClassificacaoDeLotes;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.domain.exception.LoteIndisponivelException;
import br.com.icb.ingressos.domain.exception.QuantidadeDeIngressosInvalidaException;
import br.com.icb.ingressos.domain.exception.QuantidadeIndisponivelException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort.DadosCobranca;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

/**
 * Orquestra a compra de ingressos (RF-04 a RF-10).
 *
 * <p>A operação é transacional e serializa a reserva pelo bloqueio pessimista do
 * lote ({@link LoteRepositoryPort#buscarPorIdComBloqueio} — RNF-07): duas compras
 * concorrentes do mesmo lote esperam a transação anterior, o que impede a
 * quantidade disponível de ficar negativa e o mesmo ingresso de ser reservado
 * duas vezes. Ou o pedido reserva todos os ingressos pedidos, ou nenhum.
 */
@Service
public class ComprarIngressoService implements ComprarIngressoUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final PedidoRepositoryPort pedidoRepository;
    private final PagamentoRepositoryPort pagamentoRepository;
    private final GatewayPagamentoPort gatewayPagamento;
    private final Clock clock;
    private final int maxIngressosPorPedido;

    public ComprarIngressoService(UsuarioRepositoryPort usuarioRepository,
                                  EventoRepositoryPort eventoRepository,
                                  LoteRepositoryPort loteRepository,
                                  IngressoRepositoryPort ingressoRepository,
                                  PedidoRepositoryPort pedidoRepository,
                                  PagamentoRepositoryPort pagamentoRepository,
                                  GatewayPagamentoPort gatewayPagamento,
                                  Clock clock,
                                  @Value("${app.compra.max-ingressos-por-pedido}") int maxIngressosPorPedido) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
        this.ingressoRepository = ingressoRepository;
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.gatewayPagamento = gatewayPagamento;
        this.clock = clock;
        this.maxIngressosPorPedido = maxIngressosPorPedido;
    }

    @Override
    @Transactional
    public ResultadoCompra comprar(ComprarIngressoCommand comando) {
        var participantes = validarParticipantes(comando.participantes());
        var cliente = usuarioRepository.buscarPorId(comando.usuarioId())
                .filter(Usuario::possuiConta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", comando.usuarioId()));
        var lote = loteRepository.buscarPorIdComBloqueio(comando.loteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lote", comando.loteId()));
        garantirEventoAtivo(lote.getEventoId());
        garantirQueEhOLoteAtivo(lote);
        var quantidade = participantes.size();
        if (lote.getQuantidadeDisponivel() < quantidade) {
            throw new QuantidadeIndisponivelException(lote.getQuantidadeDisponivel());
        }
        var ingressos = ingressoRepository.listarDisponiveisDoLote(lote.getId(), quantidade);
        if (ingressos.size() < quantidade) {
            throw new QuantidadeIndisponivelException(ingressos.size());
        }

        var valorTotal = lote.getPreco().multiply(BigDecimal.valueOf(quantidade));
        var pedido = pedidoRepository.salvar(
                Pedido.novo(cliente.getId(), lote.getId(), quantidade, agora(), valorTotal));
        var ingressoIds = new ArrayList<Long>(quantidade);
        for (var i = 0; i < quantidade; i++) {
            lote.reservarUnidade();
            Ingresso ingresso = ingressos.get(i);
            ingresso.reservar(pedido.getId(), participantes.get(i));
            ingressoIds.add(ingressoRepository.salvar(ingresso).getId());
        }
        loteRepository.salvar(lote);

        var cobranca = solicitarCobranca(comando, pedido, cliente);
        return new ResultadoCompra(
                pedido.getId(),
                List.copyOf(ingressoIds),
                pedido.getValorTotal(),
                pedido.getStatus(),
                cobranca.urlPagamento(),
                cobranca.qrCodePix());
    }

    /** RN-1: de 1 até o limite configurado, um nome não vazio por ingresso. */
    private List<String> validarParticipantes(List<String> participantes) {
        if (participantes == null || participantes.isEmpty()
                || participantes.size() > maxIngressosPorPedido) {
            throw new QuantidadeDeIngressosInvalidaException(maxIngressosPorPedido);
        }
        if (participantes.stream().anyMatch(nome -> nome == null || nome.isBlank())) {
            throw new IllegalArgumentException("Informe o nome de cada participante.");
        }
        return participantes.stream().map(String::strip).toList();
    }

    private void garantirEventoAtivo(Long eventoId) {
        var evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", eventoId));
        if (evento.cancelado()) {
            throw new TransicaoInvalidaException("As vendas deste evento foram encerradas.");
        }
    }

    /**
     * Virada de lote ({@code docs/viradaDeLote.md}): só o lote ativo do evento
     * vende. Recusa compras de lote agendado, encerrado ou ainda na fila. O lote
     * travado entra na classificação com o estoque recém-lido.
     */
    private void garantirQueEhOLoteAtivo(Lote loteTravado) {
        var outrosLotes = loteRepository.listarPorEvento(loteTravado.getEventoId()).stream()
                .filter(lote -> !Objects.equals(lote.getId(), loteTravado.getId()));
        var lotesDoEvento = Stream.concat(Stream.of(loteTravado), outrosLotes).toList();
        var classificacao = ClassificacaoDeLotes.de(lotesDoEvento, agora());
        if (classificacao.ativoEh(loteTravado.getId())) {
            return;
        }
        throw switch (classificacao.statusDe(loteTravado.getId())) {
            case ESGOTADO -> new IngressoEsgotadoException(loteTravado.getId());
            case ENCERRADO -> new LoteIndisponivelException("As vendas deste lote foram encerradas.");
            case AGENDADO -> new LoteIndisponivelException("As vendas deste lote ainda não começaram.");
            default -> new LoteIndisponivelException("Este lote ainda não está disponível.");
        };
    }

    private GatewayPagamentoPort.CobrancaCriada solicitarCobranca(ComprarIngressoCommand comando,
                                                                  Pedido pedido, Usuario cliente) {
        var pagamento = pagamentoRepository.salvar(
                Pagamento.pendente(pedido.getId(), pedido.getValorTotal(), comando.metodoPagamento()));
        var cobranca = gatewayPagamento.criarCobranca(new DadosCobranca(
                pedido.getId(), pedido.getValorTotal(), comando.metodoPagamento(),
                cliente.getNome(), cliente.getEmail()));
        pagamento.vincularCobranca(cobranca.referenciaGateway());
        pagamentoRepository.salvar(pagamento);
        return cobranca;
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
