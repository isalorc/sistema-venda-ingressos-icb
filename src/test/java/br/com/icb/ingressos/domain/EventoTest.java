package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EventoTest {

    private static final LocalDateTime DATA_EVENTO = LocalDateTime.of(2026, 12, 24, 20, 0);

    @Test
    void criaEventoValido() {
        var evento = Evento.novo("Ceia de Natal", "Culto especial", DATA_EVENTO);

        assertThat(evento.getId()).isNull();
        assertThat(evento.getNome()).isEqualTo("Ceia de Natal");
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Evento.novo("", "desc", DATA_EVENTO));
    }

    @Test
    void rejeitaDataHoraNula() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Evento.novo("Ceia", "desc", null));
    }

    @Test
    void jaOcorreuComparaComAReferencia() {
        var evento = Evento.novo("Ceia", "desc", DATA_EVENTO);

        assertThat(evento.jaOcorreu(DATA_EVENTO.minusDays(1))).isFalse();
        assertThat(evento.jaOcorreu(DATA_EVENTO.plusDays(1))).isTrue();
    }
}
