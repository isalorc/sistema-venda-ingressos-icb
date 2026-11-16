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
import br.com.icb.ingressos.domain.enums.StatusVendas;
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

    private void eventoUnico() {
        when(eventoRepository.listarTodos()).thenReturn(List.of(
                Evento.reconstituir(1L, "Congresso", "descrição", AGORA.plusDays(30))));
    }

    @Test
    void precoEDisponibilidadeVemDoLoteAtivoNaoDaSomaDosLotes() {
        eventoUnico();
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "1º Lote", new BigDecimal("80.00"), 50, 5),
                Lote.reconstituir(11L, 1L, "2º Lote", new BigDecimal("50.00"), 50, 3)));

        var disponiveis = service.listar();

        assertThat(disponiveis).hasSize(1);
        assertThat(disponiveis.get(0).statusVendas()).isEqualTo(StatusVendas.A_VENDA);
        assertThat(disponiveis.get(0).menorPreco()).isEqualByComparingTo("80.00");
        assertThat(disponiveis.get(0).ingressosDisponiveis()).isEqualTo(5);
        assertThat(disponiveis.get(0).aberturaVendas()).isNull();
    }

    @Test
    void quandoPrimeiroLoteEsgotaOSegundoAssumeComoAtivo() {
        eventoUnico();
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "1º Lote", new BigDecimal("80.00"), 50, 0),
                Lote.reconstituir(11L, 1L, "2º Lote", new BigDecimal("50.00"), 50, 20)));

        var evento = service.listar().get(0);

        assertThat(evento.menorPreco()).isEqualByComparingTo("50.00");
        assertThat(evento.ingressosDisponiveis()).isEqualTo(20);
    }

    @Test
    void eventoComTodosOsLotesEsgotadosNaoAparece() {
        eventoUnico();
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Único", new BigDecimal("30.00"), 50, 0)));

        assertThat(service.listar()).isEmpty();
    }

    @Test
    void eventoSoComLoteAgendadoApareceComoAgendadaEDadosDoProximo() {
        eventoUnico();
        var abertura = AGORA.plusDays(10);
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "1º Lote", new BigDecimal("60.00"), 100, 100, abertura, null)));

        var evento = service.listar().get(0);

        assertThat(evento.statusVendas()).isEqualTo(StatusVendas.AGENDADA);
        assertThat(evento.aberturaVendas()).isEqualTo(abertura);
        assertThat(evento.menorPreco()).isEqualByComparingTo("60.00");
        assertThat(evento.ingressosDisponiveis()).isEqualTo(100);
    }

    @Test
    void omiteEventoJaOcorrido() {
        when(eventoRepository.listarTodos()).thenReturn(List.of(
                Evento.reconstituir(1L, "Passado", null, AGORA.minusDays(1))));

        assertThat(service.listar()).isEmpty();
    }

    @Test
    void omiteEventoSemLote() {
        eventoUnico();
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
