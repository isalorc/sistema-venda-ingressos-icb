package br.com.icb.ingressos.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.ports.in.ExpirarReservasUseCase;

@ExtendWith(MockitoExtension.class)
class ExpiracaoDeReservasSchedulerTest {

    @Mock private ExpirarReservasUseCase expirarReservas;
    @InjectMocks private ExpiracaoDeReservasScheduler scheduler;

    @Test
    void delegaAVarreduraAoCasoDeUso() {
        when(expirarReservas.expirarReservasVencidas()).thenReturn(2);

        scheduler.varrerReservasVencidas();

        verify(expirarReservas).expirarReservasVencidas();
    }

    @Test
    void falhaNoCasoDeUsoNaoPropaga() {
        when(expirarReservas.expirarReservasVencidas())
                .thenThrow(new IllegalStateException("banco fora do ar"));

        assertThatCode(scheduler::varrerReservasVencidas).doesNotThrowAnyException();
    }
}
