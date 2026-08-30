package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase.CriarLoteCommand;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@ExtendWith(MockitoExtension.class)
class CriarLoteServiceTest {

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;

    @InjectMocks private CriarLoteService service;

    private static CriarLoteCommand comando(int quantidade) {
        return new CriarLoteCommand(1L, "Inteira", new BigDecimal("50.00"), quantidade);
    }

    @Test
    void criaOLoteEGeraUmIngressoDisponivelPorUnidade() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(
                Evento.reconstituir(1L, "Congresso", null, LocalDateTime.of(2026, 12, 1, 20, 0))));
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
    void eventoInexistenteLancaRecursoNaoEncontradoSemCriarNada() {
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(comando(3)));

        verify(loteRepository, never()).salvar(any());
        verify(ingressoRepository, never()).salvarTodos(any());
    }
}
