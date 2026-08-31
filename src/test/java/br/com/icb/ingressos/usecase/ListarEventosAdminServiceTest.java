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
class ListarEventosAdminServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;

    private ListarEventosAdminService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ListarEventosAdminService(eventoRepository, loteRepository, clock);
    }

    @Test
    void listaTodosOsEventosDoMaisRecenteParaOMaisAntigoComResumoDosLotes() {
        var futuro = Evento.reconstituir(1L, "Congresso", "anual", AGORA.plusDays(10));
        var passado = Evento.reconstituir(2L, "Retiro", null, AGORA.minusDays(5));
        when(eventoRepository.listarTodos()).thenReturn(List.of(futuro, passado));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("60.00"), 100, 80),
                Lote.reconstituir(11L, 1L, "Meia", new BigDecimal("30.00"), 50, 50)));
        when(loteRepository.listarPorEvento(2L)).thenReturn(List.of());

        var eventos = service.listar();

        assertThat(eventos).extracting("id").containsExactly(2L, 1L);

        var congresso = eventos.get(1);
        assertThat(congresso.jaOcorreu()).isFalse();
        assertThat(congresso.quantidadeLotes()).isEqualTo(2);
        assertThat(congresso.ingressosTotais()).isEqualTo(150);
        assertThat(congresso.ingressosDisponiveis()).isEqualTo(130);
        assertThat(congresso.statusVendas()).isEqualTo(StatusVendas.A_VENDA);

        var retiro = eventos.get(0);
        assertThat(retiro.jaOcorreu()).isTrue();
        assertThat(retiro.quantidadeLotes()).isZero();
        assertThat(retiro.ingressosTotais()).isZero();
        assertThat(retiro.ingressosDisponiveis()).isZero();
        assertThat(retiro.statusVendas()).isEqualTo(StatusVendas.ENCERRADA);
    }

    @Test
    void semEventosDevolveListaVazia() {
        when(eventoRepository.listarTodos()).thenReturn(List.of());

        assertThat(service.listar()).isEmpty();
    }
}
