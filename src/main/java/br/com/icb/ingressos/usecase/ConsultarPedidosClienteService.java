package br.com.icb.ingressos.usecase;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConsultarPedidosClienteUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

@Service
public class ConsultarPedidosClienteService implements ConsultarPedidosClienteUseCase {

    private final PedidoRepositoryPort pedidoRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final LoteRepositoryPort loteRepository;
    private final EventoRepositoryPort eventoRepository;
    private final PagamentoRepositoryPort pagamentoRepository;
    private final Duration ttlReserva;

    public ConsultarPedidosClienteService(PedidoRepositoryPort pedidoRepository,
                                          IngressoRepositoryPort ingressoRepository,
                                          LoteRepositoryPort loteRepository,
                                          EventoRepositoryPort eventoRepository,
                                          PagamentoRepositoryPort pagamentoRepository,
                                          @Value("${app.reserva.ttl}") Duration ttlReserva) {
        this.pedidoRepository = pedidoRepository;
        this.ingressoRepository = ingressoRepository;
        this.loteRepository = loteRepository;
        this.eventoRepository = eventoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.ttlReserva = ttlReserva;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResumo> listar(Long usuarioId) {
        return pedidoRepository.listarPorUsuario(usuarioId).stream()
                .map(this::resumir)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoDetalhe detalhar(Long usuarioId, Long pedidoId) {
        var pedido = pedidoRepository.buscarPorId(pedidoId)
                .filter(encontrado -> Objects.equals(encontrado.getUsuarioId(), usuarioId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", pedidoId));
        var pagamento = pagamentoRepository.buscarPorPedido(pedido.getId());
        var ingressos = ingressoRepository.listarPorPedido(pedido.getId()).stream()
                .sorted(Comparator.comparing(Ingresso::getId))
                .map(ingresso -> new IngressoDoPedido(ingresso.getId(), ingresso.getNomeParticipante(),
                        ingresso.getStatus(), ingresso.getCodigoQr()))
                .toList();
        return new PedidoDetalhe(
                resumir(pedido),
                pagamento.map(p -> p.getMetodo()).orElse(null),
                pagamento.map(p -> p.getStatus()).orElse(null),
                ingressos);
    }

    private PedidoResumo resumir(Pedido pedido) {
        var expiraEm = pedido.getStatus() == StatusPedido.PENDENTE
                ? pedido.getDataPedido().plus(ttlReserva)
                : null;
        return new PedidoResumo(
                pedido.getId(),
                pedido.getDataPedido(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                pedido.getQuantidadeIngressos(),
                eventoDo(pedido),
                expiraEm);
    }

    private EventoDoPedido eventoDo(Pedido pedido) {
        if (pedido.getLoteId() == null) {
            return null;
        }
        return loteRepository.buscarPorId(pedido.getLoteId())
                .flatMap(lote -> eventoRepository.buscarPorId(lote.getEventoId())
                        .map(evento -> new EventoDoPedido(
                                evento.getId(),
                                evento.getNome(),
                                evento.getDataHora(),
                                evento.getDataFim(),
                                evento.getImagemUrl(),
                                lote.getId(),
                                lote.getNome())))
                .orElse(null);
    }
}
