package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ListarEventosDisponiveisServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;

    private ListarEventosDisponiveisService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ListarEventosDisponiveisService(eventoRepository, loteRepository, clock);
    }

    @Test
    void resumeOMenorPrecoEOTotalDisponivelDosLotesComEstoque() {
        var evento = Evento.reconstituir(1L, "Congresso", "descrição", AGORA.plusDays(10));
        when(eventoRepository.listarTodos()).thenReturn(List.of(evento));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Lote 1", new BigDecimal("80.00"), 50, 5),
                Lote.reconstituir(11L, 1L, "Lote 2", new BigDecimal("50.00"), 50, 3),
                Lote.reconstituir(12L, 1L, "Esgotado", new BigDecimal("30.00"), 50, 0)));

        var disponiveis = service.listar();

        assertThat(disponiveis).hasSize(1);
        assertThat(disponiveis.get(0).menorPreco()).isEqualByComparingTo("50.00");
        assertThat(disponiveis.get(0).ingressosDisponiveis()).isEqualTo(8);
    }

    @Test
    void omiteEventoJaOcorrido() {
        when(eventoRepository.listarTodos()).thenReturn(List.of(
                Evento.reconstituir(1L, "Passado", null, AGORA.minusDays(1))));

        assertThat(service.listar()).isEmpty();
    }

    @Test
    void omiteEventoSemLoteComEstoque() {
        var evento = Evento.reconstituir(1L, "Sem estoque", null, AGORA.plusDays(10));
        when(eventoRepository.listarTodos()).thenReturn(List.of(evento));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Esgotado", new BigDecimal("30.00"), 50, 0)));

        assertThat(service.listar()).isEmpty();
    }
}
