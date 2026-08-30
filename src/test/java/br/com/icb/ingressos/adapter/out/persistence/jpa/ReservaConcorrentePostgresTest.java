package br.com.icb.ingressos.adapter.out.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ComprarIngressoCommand;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

/**
 * RNF-30: sob concorrência real, o estoque de um lote nunca fica negativo e o
 * mesmo ingresso não é reservado duas vezes. Várias threads disputam um lote com
 * poucas unidades; o bloqueio pessimista de {@code buscarPorIdComBloqueio}
 * (SELECT ... FOR UPDATE) serializa a reserva. Este teste falharia com o adapter
 * em memória do MVP, que não tem lock — por isso vive sobre PostgreSQL.
 */
class ReservaConcorrentePostgresTest extends IntegracaoPostgresTest {

    @Autowired private ComprarIngressoUseCase comprarIngresso;
    @Autowired private EventoRepositoryPort eventoRepository;
    @Autowired private LoteRepositoryPort loteRepository;
    @Autowired private IngressoRepositoryPort ingressoRepository;

    @Test
    void vinteComprasSimultaneasDeUmLoteComCincoUnidadesVendemExatamenteCinco() throws Exception {
        int estoque = 5;
        int concorrentes = 20;

        var evento = eventoRepository.salvar(
                Evento.novo("Retiro", null, LocalDateTime.now().plusDays(20)));
        var lote = loteRepository.salvar(
                Lote.novo(evento.getId(), "Inteira", new BigDecimal("40.00"), estoque));
        ingressoRepository.salvarTodos(IntStream.range(0, estoque)
                .mapToObj(i -> Ingresso.disponivel(evento.getId(), lote.getId()))
                .toList());

        var barreira = new CyclicBarrier(concorrentes);
        var executor = Executors.newFixedThreadPool(concorrentes);
        try {
            List<Callable<Boolean>> tarefas = IntStream.range(0, concorrentes)
                    .<Callable<Boolean>>mapToObj(i -> () -> {
                        barreira.await(10, TimeUnit.SECONDS);
                        try {
                            comprarIngresso.comprar(new ComprarIngressoCommand(
                                    lote.getId(), "Cliente " + i, "cliente" + i + "@exemplo.com",
                                    "1199999000" + (i % 10), MetodoPagamento.PIX));
                            return true;
                        } catch (RuntimeException esgotadoOuConflito) {
                            return false;
                        }
                    })
                    .toList();

            var sucessos = executor.invokeAll(tarefas, 30, TimeUnit.SECONDS).stream()
                    .filter(ReservaConcorrentePostgresTest::valor)
                    .count();

            assertThat(sucessos).isEqualTo(estoque);
        } finally {
            executor.shutdownNow();
        }

        assertThat(loteRepository.buscarPorId(lote.getId()).orElseThrow().getQuantidadeDisponivel())
                .isZero();
        assertThat(ingressoRepository.buscarPrimeiroDisponivelDoLote(lote.getId())).isEmpty();
    }

    private static boolean valor(Future<Boolean> futuro) {
        try {
            return Boolean.TRUE.equals(futuro.get());
        } catch (Exception e) {
            return false;
        }
    }
}
