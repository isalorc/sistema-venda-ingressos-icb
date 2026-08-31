package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import br.com.icb.ingressos.ports.in.GerenciarEventoUseCase.DadosDoEvento;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class GerenciarEventoServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final LocalDateTime DATA = AGORA.plusMonths(3);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;

    private GerenciarEventoService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new GerenciarEventoService(eventoRepository, loteRepository, ingressoRepository, clock);
    }

    private void eventoExiste(Evento evento) {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(evento));
    }

    private static DadosDoEvento dados(LocalDateTime dataHora) {
        return new DadosDoEvento("Congresso 2027", "nova descrição", dataHora, null, null);
    }

    @Test
    void editarSubstituiOsDadosEPersiste() {
        eventoExiste(Evento.reconstituir(1L, "Congresso", "antiga", DATA));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of());

        service.editar(1L, dados(DATA.plusDays(1)));

        var salvo = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).salvar(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("Congresso 2027");
        assertThat(salvo.getValue().getDataHora()).isEqualTo(DATA.plusDays(1));
    }

    @Test
    void editarEventoCanceladoLanca409() {
        eventoExiste(Evento.reconstituir(1L, "Congresso", null, DATA, null, null, AGORA.minusDays(1)));

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> service.editar(1L, dados(DATA)));

        verify(eventoRepository, never()).salvar(any());
    }

    @Test
    void editarComDataQueDeixaJanelaDeLoteForaDoEventoLanca422() {
        eventoExiste(Evento.reconstituir(1L, "Congresso", null, DATA));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Lote", new BigDecimal("50.00"), 100, 100,
                        null, DATA.minusDays(1))));

        assertThatExceptionOfType(PeriodoDeVendaInvalidoException.class)
                .isThrownBy(() -> service.editar(1L, dados(DATA.minusDays(10))));

        verify(eventoRepository, never()).salvar(any());
    }

    @Test
    void cancelarMarcaOEventoEPersiste() {
        var evento = Evento.reconstituir(1L, "Congresso", null, DATA);
        eventoExiste(evento);

        service.cancelar(1L);

        assertThat(evento.cancelado()).isTrue();
        verify(eventoRepository).salvar(evento);
    }

    @Test
    void excluirRemoveLotesIngressosEEventoQuandoNadaFoiVendido() {
        eventoExiste(Evento.reconstituir(1L, "Congresso", null, DATA));
        when(ingressoRepository.contarPorEventoNosStatus(eq(1L), anyCollection())).thenReturn(0L);
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Lote", new BigDecimal("50.00"), 100, 100)));

        service.excluir(1L);

        verify(ingressoRepository).excluirPorLote(10L);
        verify(loteRepository).excluir(10L);
        verify(eventoRepository).excluir(1L);
    }

    @Test
    void excluirComIngressoReservadoOuVendidoLanca409() {
        eventoExiste(Evento.reconstituir(1L, "Congresso", null, DATA));
        when(ingressoRepository.contarPorEventoNosStatus(eq(1L), anyCollection())).thenReturn(3L);

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> service.excluir(1L));

        verify(eventoRepository, never()).excluir(any());
    }

    @Test
    void editarEventoInexistenteLanca404() {
        when(eventoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.editar(99L, dados(DATA)));
    }
}
