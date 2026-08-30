package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConsultarEventoServiceTest {

    private static final LocalDateTime DATA = LocalDateTime.of(2026, 12, 1, 20, 0);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;

    @InjectMocks private ConsultarEventoService service;

    @Test
    void retornaOEventoComTodosOsLotesInclusiveEsgotados() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(
                Evento.reconstituir(1L, "Congresso", "descrição", DATA)));
        when(loteRepository.listarPorEvento(1L)).thenReturn(List.of(
                Lote.reconstituir(10L, 1L, "Inteira", new BigDecimal("50.00"), 100, 40),
                Lote.reconstituir(11L, 1L, "Esgotado", new BigDecimal("30.00"), 100, 0)));

        var detalhe = service.consultar(1L);

        assertThat(detalhe.eventoId()).isEqualTo(1L);
        assertThat(detalhe.nome()).isEqualTo("Congresso");
        assertThat(detalhe.lotes()).hasSize(2);
        assertThat(detalhe.lotes().get(0).quantidadeDisponivel()).isEqualTo(40);
        assertThat(detalhe.lotes().get(1).quantidadeDisponivel()).isZero();
    }

    @Test
    void eventoInexistenteLancaRecursoNaoEncontrado() {
        when(eventoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.consultar(99L));
    }
}
