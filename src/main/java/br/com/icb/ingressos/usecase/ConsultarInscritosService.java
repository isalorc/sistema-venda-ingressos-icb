package br.com.icb.ingressos.usecase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConsultarInscritosUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

/**
 * Lista os inscritos de um evento (RF-25): clientes com ingresso {@code VENDIDO}
 * e o respectivo pedido {@code PAGO}. Exige administrador autenticado (RF-26 — no
 * adapter).
 */
@Service
public class ConsultarInscritosService implements ConsultarInscritosUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final PedidoRepositoryPort pedidoRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final LoteRepositoryPort loteRepository;

    public ConsultarInscritosService(EventoRepositoryPort eventoRepository,
                                     IngressoRepositoryPort ingressoRepository,
                                     PedidoRepositoryPort pedidoRepository,
                                     UsuarioRepositoryPort usuarioRepository,
                                     LoteRepositoryPort loteRepository) {
        this.eventoRepository = eventoRepository;
        this.ingressoRepository = ingressoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inscrito> listarPorEvento(Long eventoId) {
        if (eventoRepository.buscarPorId(eventoId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Evento", eventoId);
        }

        return ingressoRepository.listarVendidosDoEvento(eventoId).stream()
                .map(this::paraInscritoSePago)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Inscrito> paraInscritoSePago(Ingresso ingresso) {
        var pedido = pedidoRepository.buscarPorId(ingresso.getPedidoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", ingresso.getPedidoId()));
        if (pedido.getStatus() != StatusPedido.PAGO) {
            return Optional.empty();
        }

        var cliente = usuarioRepository.buscarPorId(pedido.getUsuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", pedido.getUsuarioId()));
        var lote = loteRepository.buscarPorId(ingresso.getLoteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lote", ingresso.getLoteId()));

        return Optional.of(new Inscrito(
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                lote.getId(),
                lote.getNome(),
                pedido.getId(),
                ingresso.getCodigoQr(),
                pedido.getDataPedido()));
    }
}
