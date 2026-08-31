package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import br.com.icb.ingressos.ports.in.GerenciarLoteUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Edição e exclusão de lote (ver {@code docs/gestaoDeEventos.md}). Exige
 * administrador autenticado (RF-26 — no adapter).
 */
@Service
public class GerenciarLoteService implements GerenciarLoteUseCase {

    private static final EnumSet<StatusIngresso> NAO_LIBERAVEIS =
            EnumSet.of(StatusIngresso.RESERVADO, StatusIngresso.VENDIDO);

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final Clock clock;

    public GerenciarLoteService(EventoRepositoryPort eventoRepository,
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
    public void editar(Long eventoId, Long loteId, DadosDoLote dados) {
        var evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", eventoId));
        if (evento.cancelado()) {
            throw new TransicaoInvalidaException("Não é possível editar lotes de um evento cancelado.");
        }
        var lote = loteDoEvento(eventoId, loteId);

        ValidadorDeJanelaDeVendas.validarNoLote(dados.inicioVendas(), dados.fimVendas(),
                evento.termino(), LocalDateTime.now(clock));

        var acrescimo = dados.quantidadeTotal() - lote.getQuantidadeTotal();
        var editado = loteRepository.salvar(lote.editado(dados.nome(), dados.preco(),
                dados.quantidadeTotal(), dados.inicioVendas(), dados.fimVendas()));

        if (acrescimo > 0) {
            var novos = IntStream.range(0, acrescimo)
                    .mapToObj(i -> Ingresso.disponivel(eventoId, editado.getId()))
                    .toList();
            ingressoRepository.salvarTodos(novos);
        }
    }

    @Override
    @Transactional
    public void excluir(Long eventoId, Long loteId) {
        var lote = loteDoEvento(eventoId, loteId);

        if (ingressoRepository.contarPorLoteNosStatus(loteId, NAO_LIBERAVEIS) > 0) {
            throw new TransicaoInvalidaException(
                    "Não é possível excluir um lote com ingressos vendidos ou reservados.");
        }

        ingressoRepository.excluirPorLote(lote.getId());
        loteRepository.excluir(lote.getId());
    }

    private Lote loteDoEvento(Long eventoId, Long loteId) {
        var lote = loteRepository.buscarPorId(loteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lote", loteId));
        if (!lote.getEventoId().equals(eventoId)) {
            throw new RecursoNaoEncontradoException("Lote", loteId);
        }
        return lote;
    }
}
