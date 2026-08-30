package br.com.icb.ingressos.usecase;

import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Cria um lote de ingressos para um evento (RF-22) e gera os
 * {@code quantidadeTotal} ingressos correspondentes com status
 * {@code DISPONIVEL} (RF-23). Exige administrador autenticado (RF-26 — no adapter).
 */
@Service
public class CriarLoteService implements CriarLoteUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final IngressoRepositoryPort ingressoRepository;

    public CriarLoteService(EventoRepositoryPort eventoRepository,
                            LoteRepositoryPort loteRepository,
                            IngressoRepositoryPort ingressoRepository) {
        this.eventoRepository = eventoRepository;
        this.loteRepository = loteRepository;
        this.ingressoRepository = ingressoRepository;
    }

    @Override
    @Transactional
    public Long criar(CriarLoteCommand comando) {
        var evento = eventoRepository.buscarPorId(comando.eventoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", comando.eventoId()));

        var lote = loteRepository.salvar(Lote.novo(
                evento.getId(), comando.nome(), comando.preco(), comando.quantidadeTotal()));

        var ingressos = IntStream.range(0, comando.quantidadeTotal())
                .mapToObj(indice -> Ingresso.disponivel(evento.getId(), lote.getId()))
                .toList();
        ingressoRepository.salvarTodos(ingressos);

        return lote.getId();
    }
}
