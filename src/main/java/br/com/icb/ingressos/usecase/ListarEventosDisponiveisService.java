package br.com.icb.ingressos.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Lista os eventos disponíveis para compra (RF-01).
 *
 * <p>Um evento entra na lista quando ainda não ocorreu (RF-03) e tem pelo menos
 * um lote com ingressos disponíveis. O menor preço e o total de ingressos
 * disponíveis consideram apenas os lotes com estoque.
 */
@Service
public class ListarEventosDisponiveisService implements ListarEventosDisponiveisUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final Clock clock;

    public ListarEventosDisponiveisService(EventoRepositoryPort eventoRepository,
                                           LoteRepositoryPort loteRepository,
                                           Clock clock) {
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDisponivel> listar() {
        var agora = LocalDateTime.now(clock);
        return eventoRepository.listarTodos().stream()
                .filter(evento -> !evento.jaOcorreu(agora))
                .map(this::resumirSeDisponivel)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<EventoDisponivel> resumirSeDisponivel(Evento evento) {
        var lotesComEstoque = loteRepository.listarPorEvento(evento.getId()).stream()
                .filter(lote -> lote.getQuantidadeDisponivel() > 0)
                .toList();

        if (lotesComEstoque.isEmpty()) {
            return Optional.empty();
        }

        var menorPreco = lotesComEstoque.stream()
                .map(Lote::getPreco)
                .min(BigDecimal::compareTo)
                .orElseThrow();
        var ingressosDisponiveis = lotesComEstoque.stream()
                .mapToInt(Lote::getQuantidadeDisponivel)
                .sum();

        return Optional.of(new EventoDisponivel(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getDataHora(),
                menorPreco,
                ingressosDisponiveis));
    }
}
