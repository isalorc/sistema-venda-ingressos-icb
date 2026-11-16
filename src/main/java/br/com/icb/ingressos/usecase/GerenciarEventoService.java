package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import br.com.icb.ingressos.ports.in.GerenciarEventoUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Edição, cancelamento e exclusão de evento (ver {@code docs/gestaoDeEventos.md}).
 * Exige administrador autenticado (RF-26 — no adapter).
 */
@Service
public class GerenciarEventoService implements GerenciarEventoUseCase {

    private static final EnumSet<StatusIngresso> NAO_LIBERAVEIS =
            EnumSet.of(StatusIngresso.RESERVADO, StatusIngresso.VENDIDO);

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final Clock clock;

    public GerenciarEventoService(EventoRepositoryPort eventoRepository,
                                  LoteRepositoryPort loteRepository,
                                  IngressoRepositoryPort ingressoRepository,
                                  Clock clock) {
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
        this.ingressoRepository = ingressoRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void editar(Long eventoId, DadosDoEvento dados) {
        var evento = buscar(eventoId);

        var editado = evento.editado(dados.nome(), dados.descricao(), dados.dataHora(),
                dados.dataFim(), dados.imagemUrl());

        // A nova data não pode deixar a janela de vendas de nenhum lote fora do evento.
        for (var lote : loteRepository.listarPorEvento(eventoId)) {
            ValidadorDeJanelaDeVendas.validarDentroDoEvento(
                    lote.getInicioVendas(), lote.getFimVendas(), editado.termino());
        }

        eventoRepository.salvar(editado);
    }

    @Override
    @Transactional
    public void cancelar(Long eventoId) {
        var evento = buscar(eventoId);
        evento.cancelar(LocalDateTime.now(clock));
        eventoRepository.salvar(evento);
    }

    @Override
    @Transactional
    public void excluir(Long eventoId) {
        var evento = buscar(eventoId);

        if (ingressoRepository.contarPorEventoNosStatus(eventoId, NAO_LIBERAVEIS) > 0) {
            throw new TransicaoInvalidaException(
                    "Não é possível excluir um evento com ingressos vendidos ou reservados. "
                            + "Cancele o evento.");
        }

        for (var lote : loteRepository.listarPorEvento(eventoId)) {
            ingressoRepository.excluirPorLote(lote.getId());
            loteRepository.excluir(lote.getId());
        }
        eventoRepository.excluir(evento.getId());
    }

    private Evento buscar(Long eventoId) {
        return eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", eventoId));
    }
}
