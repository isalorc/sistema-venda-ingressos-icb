package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ExpirarReservasUseCase;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

/**
 * Expira as reservas não pagas (RF-17, RF-18 / RN-2).
 *
 * <p>Disparado periodicamente pelo scheduler. Para cada {@code Pedido} ainda
 * {@code PENDENTE} criado há mais tempo que {@code app.reserva.ttl} (15 min por
 * padrão): {@code Pedido} &rarr; EXPIRADO, o ingresso volta a {@code DISPONIVEL},
 * o estoque do lote é reposto e o pagamento pendente é cancelado.
 */
@Service
public class ExpirarReservasService implements ExpirarReservasUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpirarReservasService.class);

    private final PedidoRepositoryPort pedidoRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final LoteRepositoryPort loteRepository;
    private final PagamentoRepositoryPort pagamentoRepository;
    private final Clock clock;
    private final Duration ttlReserva;

    public ExpirarReservasService(PedidoRepositoryPort pedidoRepository,
                                  IngressoRepositoryPort ingressoRepository,
                                  LoteRepositoryPort loteRepository,
                                  PagamentoRepositoryPort pagamentoRepository,
                                  Clock clock,
                                  @Value("${app.reserva.ttl}") Duration ttlReserva) {
        this.pedidoRepository = pedidoRepository;
        this.ingressoRepository = ingressoRepository;
        this.loteRepository = loteRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.clock = clock;
        this.ttlReserva = ttlReserva;
    }

    @Override
    @Transactional
    public int expirarReservasVencidas() {
        var limite = LocalDateTime.now(clock).minus(ttlReserva);
        var vencidos = pedidoRepository.listarPendentesCriadosAntesDe(limite);

        for (var pedido : vencidos) {
            expirar(pedido);
        }

        if (!vencidos.isEmpty()) {
            log.info("{} reserva(s) expirada(s) (limite {}).", vencidos.size(), limite);
        }
        return vencidos.size();
    }

    private void expirar(Pedido pedido) {
        pedido.expirar();
        pedidoRepository.salvar(pedido);

        for (var ingresso : ingressoRepository.listarPorPedido(pedido.getId())) {
            ingresso.liberarReserva();
            ingressoRepository.salvar(ingresso);

            var lote = loteRepository.buscarPorId(ingresso.getLoteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Lote", ingresso.getLoteId()));
            lote.liberarUnidade();
            loteRepository.salvar(lote);
        }

        pagamentoRepository.buscarPorPedido(pedido.getId())
                .filter(pagamento -> pagamento.getStatus() == StatusPagamento.PENDENTE)
                .ifPresent(pagamento -> {
                    pagamento.cancelar();
                    pagamentoRepository.salvar(pagamento);
                });
    }
}
