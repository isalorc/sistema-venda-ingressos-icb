package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.enums.StatusLote;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConsultarEventoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 12, 1, 20, 0);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;

    private ConsultarEventoService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ConsultarEventoService(eventoRepository, loteRepository, clock);
    }

    @Test
    void retornaTodosOsLotesComOStatusDerivadoDaViradaDeLote() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(
                Evento.reconstituir(1L, "Congresso", "descrição", DATA)));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "1º Lote", new BigDecimal("50.00"), 100, 0),
                Lote.reconstituir(11L, 1L, "2º Lote", new BigDecimal("80.00"), 100, 40),
                Lote.reconstituir(12L, 1L, "3º Lote", new BigDecimal("100.00"), 100, 100,
                        AGORA.plusDays(30), null)));

        var detalhe = service.consultar(1L);

        assertThat(detalhe.eventoId()).isEqualTo(1L);
        assertThat(detalhe.lotes()).hasSize(3);
        assertThat(detalhe.lotes().get(0).status()).isEqualTo(StatusLote.ESGOTADO);
        assertThat(detalhe.lotes().get(1).status()).isEqualTo(StatusLote.A_VENDA);
        assertThat(detalhe.lotes().get(2).status()).isEqualTo(StatusLote.AGENDADO);
        assertThat(detalhe.lotes().get(2).inicioVendas()).isEqualTo(AGORA.plusDays(30));
    }

    @Test
    void eventoInexistenteLancaRecursoNaoEncontrado() {
        when(eventoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.consultar(99L));
    }
}
