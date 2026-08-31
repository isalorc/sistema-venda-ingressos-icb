package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.ClassificacaoDeLotes;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Consulta os detalhes de um evento e seus lotes (RF-02).
 *
 * <p>Retorna todos os lotes do evento (inclusive esgotados e fora da janela),
 * cada um com o {@code status} derivado da virada de lote
 * ({@code docs/viradaDeLote.md}) — cabe ao consumidor decidir o que exibir e o
 * que fica selecionável.
 */
@Service
public class ConsultarEventoService implements ConsultarEventoUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final Clock clock;

    public ConsultarEventoService(EventoRepositoryPort eventoRepository,
                                  LoteRepositoryPort loteRepository,
                                  Clock clock) {
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DetalheEvento consultar(Long eventoId) {
        var evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", eventoId));

        var classificacao = ClassificacaoDeLotes.de(
                loteRepository.listarPorEvento(evento.getId()), LocalDateTime.now(clock));

        var lotes = classificacao.lotesOrdenados().stream()
                .map(lote -> new DetalheEvento.LoteDisponivel(
                        lote.getId(),
                        lote.getNome(),
                        lote.getPreco(),
                        lote.getQuantidadeDisponivel(),
                        classificacao.statusDe(lote),
                        lote.getInicioVendas(),
                        lote.getFimVendas()))
                .toList();

        return new DetalheEvento(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getDataHora(),
                lotes);
    }
}
