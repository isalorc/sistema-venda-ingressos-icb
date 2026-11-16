package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.ClassificacaoDeLotes;
import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.enums.StatusVendas;
import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Lista os eventos disponíveis para compra (RF-01).
 *
 * <p>Um evento entra na lista quando ainda não ocorreu (RF-03) e suas vendas não
 * estão encerradas (há um lote à venda ou agendado). O preço e a disponibilidade
 * refletem o lote ativo — ou, quando nenhum está ativo, o próximo a abrir. Regra
 * completa em {@code docs/viradaDeLote.md}.
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
                .filter(evento -> !evento.cancelado())
                .filter(evento -> !evento.jaOcorreu(agora))
                .map(evento -> resumirSeVendendo(evento, agora))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<EventoDisponivel> resumirSeVendendo(Evento evento, LocalDateTime agora) {
        var classificacao = ClassificacaoDeLotes.de(
                loteRepository.listarPorEvento(evento.getId()), agora);

        if (classificacao.statusVendas() == StatusVendas.ENCERRADA) {
            return Optional.empty();
        }

        // Não-ENCERRADA garante um lote ativo ou um próximo a abrir.
        var representante = classificacao.ativoOuProximo().orElseThrow();

        return Optional.of(new EventoDisponivel(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getDataHora(),
                evento.getDataFim(),
                evento.getImagemUrl(),
                representante.getPreco(),
                representante.getQuantidadeDisponivel(),
                classificacao.statusVendas(),
                classificacao.proximaAbertura().orElse(null)));
    }
}
