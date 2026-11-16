package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;

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

    @Test
    void jaOcorreuConsideraODataFimQuandoPresente() {
        var evento = Evento.novo("Congresso", null, DATA_EVENTO, DATA_EVENTO.plusDays(2), null);

        assertThat(evento.jaOcorreu(DATA_EVENTO.plusDays(1))).isFalse();
        assertThat(evento.jaOcorreu(DATA_EVENTO.plusDays(3))).isTrue();
    }

    @Test
    void rejeitaDataFimAntesDoInicio() {
        assertThatIllegalArgumentException().isThrownBy(() -> Evento.novo(
                "Congresso", null, DATA_EVENTO, DATA_EVENTO.minusHours(1), null));
    }

    @Test
    void rejeitaImagemUrlSemEsquemaHttp() {
        assertThatIllegalArgumentException().isThrownBy(() -> Evento.novo(
                "Congresso", null, DATA_EVENTO, null, "ftp://x/y.png"));
    }

    @Test
    void cancelarEhIdempotente() {
        var evento = Evento.reconstituir(1L, "Ceia", null, DATA_EVENTO);
        var momento = DATA_EVENTO.minusDays(3);

        evento.cancelar(momento);
        evento.cancelar(momento.plusDays(1));

        assertThat(evento.cancelado()).isTrue();
        assertThat(evento.getCanceladoEm()).isEqualTo(momento);
    }

    @Test
    void naoDeixaEditarEventoCancelado() {
        var evento = Evento.reconstituir(1L, "Ceia", null, DATA_EVENTO);
        evento.cancelar(DATA_EVENTO.minusDays(3));

        assertThatExceptionOfType(TransicaoInvalidaException.class).isThrownBy(
                () -> evento.editado("Novo nome", null, DATA_EVENTO, null, null));
    }

    @Test
    void editadoPreservaIdEEstadoDeCancelamento() {
        var evento = Evento.reconstituir(9L, "Ceia", "d", DATA_EVENTO);

        var novo = evento.editado("Ceia 2027", "outra", DATA_EVENTO.plusYears(1), null,
                "https://cdn/x.jpg");

        assertThat(novo.getId()).isEqualTo(9L);
        assertThat(novo.getNome()).isEqualTo("Ceia 2027");
        assertThat(novo.getImagemUrl()).isEqualTo("https://cdn/x.jpg");
        assertThat(novo.cancelado()).isFalse();
    }
}
