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
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import br.com.icb.ingressos.ports.in.GerenciarLoteUseCase.DadosDoLote;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class GerenciarLoteServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final LocalDateTime DATA = AGORA.plusMonths(3);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;

    private GerenciarLoteService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new GerenciarLoteService(eventoRepository, loteRepository, ingressoRepository, clock);
    }

    private void contexto(Lote lote) {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(
                Evento.reconstituir(1L, "Congresso", null, DATA)));
        when(loteRepository.buscarPorId(10L)).thenReturn(Optional.of(lote));
    }

    private static DadosDoLote dados(int quantidade) {
        return new DadosDoLote("Inteira", new BigDecimal("70.00"), quantidade, null, null);
    }

    @Test
    void editarAumentandoAQuantidadeGeraOsIngressosNovos() {
        contexto(Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("60.00"), 100, 40));
        when(loteRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(1L, 10L, dados(150));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ingresso>> novos = ArgumentCaptor.forClass(List.class);
        verify(ingressoRepository).salvarTodos(novos.capture());
        assertThat(novos.getValue()).hasSize(50)
                .allSatisfy(i -> assertThat(i.getLoteId()).isEqualTo(10L));
    }

    @Test
    void editarSemMudarAQuantidadeNaoGeraIngressos() {
        contexto(Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("60.00"), 100, 40));
        when(loteRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(1L, 10L, dados(100));

        verify(ingressoRepository, never()).salvarTodos(any());
    }

    @Test
    void editarReduzindoAQuantidadeLanca409() {
        contexto(Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("60.00"), 100, 40));

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> service.editar(1L, 10L, dados(80)));

        verify(loteRepository, never()).salvar(any());
    }

    @Test
    void editarLoteDeEventoCanceladoLanca409() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(Evento.reconstituir(
                1L, "Congresso", null, DATA, null, null, AGORA.minusDays(1))));

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> service.editar(1L, 10L, dados(120)));
    }

    @Test
    void excluirRemoveIngressosELoteQuandoNadaFoiVendido() {
        when(loteRepository.buscarPorId(10L)).thenReturn(Optional.of(
                Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("60.00"), 100, 100)));
        when(ingressoRepository.contarPorLoteNosStatus(eq(10L), anyCollection())).thenReturn(0L);

        service.excluir(1L, 10L);

        verify(ingressoRepository).excluirPorLote(10L);
        verify(loteRepository).excluir(10L);
    }

    @Test
    void excluirLoteComVendaLanca409() {
        when(loteRepository.buscarPorId(10L)).thenReturn(Optional.of(
                Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("60.00"), 100, 30)));
        when(ingressoRepository.contarPorLoteNosStatus(eq(10L), anyCollection())).thenReturn(2L);

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> service.excluir(1L, 10L));

        verify(loteRepository, never()).excluir(any());
    }
}
