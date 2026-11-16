package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase.CriarLoteCommand;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class CriarLoteServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final LocalDateTime DATA_EVENTO = LocalDateTime.of(2026, 12, 1, 20, 0);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;

    private CriarLoteService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new CriarLoteService(eventoRepository, loteRepository, ingressoRepository, clock);
    }

    private static CriarLoteCommand comando(int quantidade) {
        return new CriarLoteCommand(1L, "Inteira", new BigDecimal("50.00"), quantidade, null, null);
    }

    private static CriarLoteCommand comando(LocalDateTime inicio, LocalDateTime fim) {
        return new CriarLoteCommand(1L, "Inteira", new BigDecimal("50.00"), 10, inicio, fim);
    }

    private void eventoExiste() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(
                Evento.reconstituir(1L, "Congresso", null, DATA_EVENTO)));
    }

    @Test
    void criaOLoteEGeraUmIngressoDisponivelPorUnidade() {
        eventoExiste();
        when(loteRepository.salvar(any())).thenReturn(
                Lote.reconstituir(7L, 1L, "Inteira", new BigDecimal("50.00"), 3, 3));

        var loteId = service.criar(comando(3));

        assertThat(loteId).isEqualTo(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ingresso>> ingressos = ArgumentCaptor.forClass(List.class);
        verify(ingressoRepository).salvarTodos(ingressos.capture());
        assertThat(ingressos.getValue()).hasSize(3)
                .allSatisfy(ingresso -> {
                    assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.DISPONIVEL);
                    assertThat(ingresso.getLoteId()).isEqualTo(7L);
                    assertThat(ingresso.getEventoId()).isEqualTo(1L);
                });
    }

    @Test
    void guardaAJanelaDeVendasNoLote() {
        eventoExiste();
        var inicio = AGORA.plusDays(10);
        var fim = AGORA.plusDays(20);
        when(loteRepository.salvar(any())).thenReturn(
                Lote.reconstituir(7L, 1L, "Inteira", new BigDecimal("50.00"), 10, 10, inicio, fim));

        service.criar(comando(inicio, fim));

        var loteSalvo = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).salvar(loteSalvo.capture());
        assertThat(loteSalvo.getValue().getInicioVendas()).isEqualTo(inicio);
        assertThat(loteSalvo.getValue().getFimVendas()).isEqualTo(fim);
    }

    @Test
    void fimAntesDoInicioLanca422SemCriarNada() {
        eventoExiste();

        assertThatExceptionOfType(PeriodoDeVendaInvalidoException.class)
                .isThrownBy(() -> service.criar(comando(AGORA.plusDays(20), AGORA.plusDays(10))));

        verify(loteRepository, never()).salvar(any());
    }

    @Test
    void fimNoPassadoLanca422() {
        eventoExiste();

        assertThatExceptionOfType(PeriodoDeVendaInvalidoException.class)
                .isThrownBy(() -> service.criar(comando(null, AGORA.minusDays(1))));
    }

    @Test
    void inicioDepoisDoEventoLanca422() {
        eventoExiste();

        assertThatExceptionOfType(PeriodoDeVendaInvalidoException.class)
                .isThrownBy(() -> service.criar(comando(DATA_EVENTO.plusDays(1), null)));
    }

    @Test
    void fimDepoisDoEventoLanca422() {
        eventoExiste();

        assertThatExceptionOfType(PeriodoDeVendaInvalidoException.class)
                .isThrownBy(() -> service.criar(comando(null, DATA_EVENTO.plusHours(1))));
    }

    @Test
    void eventoInexistenteLancaRecursoNaoEncontradoSemCriarNada() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(comando(3)));

        verify(loteRepository, never()).salvar(any());
        verify(ingressoRepository, never()).salvarTodos(any());
    }
}
