package br.com.icb.ingressos.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Consulta os detalhes de um evento e seus lotes (RF-02).
 *
 * <p>Retorna todos os lotes do evento (inclusive esgotados), com nome, preço e
 * quantidade disponível — cabe ao consumidor decidir o que exibir.
 */
@Service
public class ConsultarEventoService implements ConsultarEventoUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;

    public ConsultarEventoService(EventoRepositoryPort eventoRepository,
                                  LoteRepositoryPort loteRepository) {
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DetalheEvento consultar(Long eventoId) {
        var evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", eventoId));

        var lotes = loteRepository.listarPorEvento(evento.getId()).stream()
                .map(ConsultarEventoService::paraLoteDisponivel)
                .toList();

        return new DetalheEvento(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getDataHora(),
                lotes);
    }

    private static DetalheEvento.LoteDisponivel paraLoteDisponivel(Lote lote) {
        return new DetalheEvento.LoteDisponivel(
                lote.getId(),
                lote.getNome(),
                lote.getPreco(),
                lote.getQuantidadeDisponivel());
    }
}
