package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.ClassificacaoDeLotes;
import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.ports.in.ListarEventosAdminUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Lista todos os eventos para o painel administrativo (RF-21), do mais recente
 * para o mais antigo, com o resumo dos lotes de cada um. A autenticação do
 * administrador (RF-26) é responsabilidade do adapter de entrada.
 */
@Service
public class ListarEventosAdminService implements ListarEventosAdminUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final Clock clock;

    public ListarEventosAdminService(EventoRepositoryPort eventoRepository,
                                     LoteRepositoryPort loteRepository,
                                     Clock clock) {
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoAdmin> listar() {
        var agora = LocalDateTime.now(clock);
        return eventoRepository.listarTodos().stream()
                .sorted(Comparator.comparing(Evento::getId).reversed())
                .map(evento -> resumir(evento, agora))
                .toList();
    }

    private EventoAdmin resumir(Evento evento, LocalDateTime agora) {
        var lotes = loteRepository.listarPorEvento(evento.getId());
        return new EventoAdmin(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getDataHora(),
                evento.jaOcorreu(agora),
                lotes.size(),
                lotes.stream().mapToInt(Lote::getQuantidadeTotal).sum(),
                lotes.stream().mapToInt(Lote::getQuantidadeDisponivel).sum(),
                ClassificacaoDeLotes.de(lotes, agora).statusVendas());
    }
}
