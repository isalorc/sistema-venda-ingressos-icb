package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * Cria um lote de ingressos para um evento (RF-22) e gera os
 * {@code quantidadeTotal} ingressos correspondentes com status
 * {@code DISPONIVEL} (RF-23). Exige administrador autenticado (RF-26 — no adapter).
 *
 * <p>A janela de vendas do lote é validada contra a data do evento e o momento
 * atual (virada de lote — {@code docs/viradaDeLote.md}).
 */
@Service
public class CriarLoteService implements CriarLoteUseCase {

    private final EventoRepositoryPort eventoRepository;
    private final LoteRepositoryPort loteRepository;
    private final IngressoRepositoryPort ingressoRepository;
    private final Clock clock;

    public CriarLoteService(EventoRepositoryPort eventoRepository,
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
    public Long criar(CriarLoteCommand comando) {
        var evento = eventoRepository.buscarPorId(comando.eventoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento", comando.eventoId()));

        validarJanelaDeVendas(comando, evento.getDataHora());

        var lote = loteRepository.salvar(Lote.novo(
                evento.getId(), comando.nome(), comando.preco(), comando.quantidadeTotal(),
                comando.inicioVendas(), comando.fimVendas()));

        var ingressos = IntStream.range(0, comando.quantidadeTotal())
                .mapToObj(indice -> Ingresso.disponivel(evento.getId(), lote.getId()))
                .toList();
        ingressoRepository.salvarTodos(ingressos);

        return lote.getId();
    }

    private void validarJanelaDeVendas(CriarLoteCommand comando, LocalDateTime dataEvento) {
        var inicio = comando.inicioVendas();
        var fim = comando.fimVendas();
        var agora = LocalDateTime.now(clock);

        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O fim das vendas não pode ser anterior ao início.");
        }
        if (fim != null && fim.isBefore(agora)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O fim das vendas não pode estar no passado.");
        }
        if (inicio != null && inicio.isAfter(dataEvento)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O início das vendas não pode ser depois da data do evento.");
        }
        if (fim != null && fim.isAfter(dataEvento)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O fim das vendas não pode ser depois da data do evento.");
        }
    }
}
