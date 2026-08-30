package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;

class IngressoTest {

    private static Ingresso novoDisponivel() {
        return Ingresso.disponivel(1L, 1L);
    }

    @Test
    void ingressoNovoNasceDisponivelSemPedidoNemCodigo() {
        var ingresso = novoDisponivel();

        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.DISPONIVEL);
        assertThat(ingresso.getPedidoId()).isNull();
        assertThat(ingresso.getCodigoQr()).isNull();
    }

    @Test
    void fluxoFeliz_reservarConfirmarUtilizar() {
        var ingresso = novoDisponivel();

        ingresso.reservar(10L);
        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.RESERVADO);
        assertThat(ingresso.getPedidoId()).isEqualTo(10L);
        assertThat(ingresso.getCodigoQr()).as("sem código antes do pagamento (RN-6)").isNull();

        ingresso.confirmarVenda("QR-123");
        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.VENDIDO);
        assertThat(ingresso.getCodigoQr()).isEqualTo("QR-123");

        ingresso.utilizar();
        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.UTILIZADO);
    }

    @Test
    void liberarReservaVoltaParaDisponivelEDesvinculaPedido() {
        var ingresso = novoDisponivel();
        ingresso.reservar(10L);

        ingresso.liberarReserva();

        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.DISPONIVEL);
        assertThat(ingresso.getPedidoId()).isNull();
    }

    @Test
    void reservarExigePedido() {
        assertThatIllegalArgumentException().isThrownBy(() -> novoDisponivel().reservar(null));
    }

    @Test
    void naoReservaIngressoJaReservado() {
        var ingresso = novoDisponivel();
        ingresso.reservar(10L);

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> ingresso.reservar(11L));
    }

    @Test
    void naoConfirmaVendaSemReserva() {
        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> novoDisponivel().confirmarVenda("QR-123"));
    }

    @Test
    void confirmarVendaExigeCodigoNaoVazio() {
        var ingresso = novoDisponivel();
        ingresso.reservar(10L);

        assertThatIllegalArgumentException().isThrownBy(() -> ingresso.confirmarVenda("  "));
    }

    @Test
    void naoUtilizaIngressoNaoVendido() {
        var ingresso = novoDisponivel();
        ingresso.reservar(10L);

        assertThatExceptionOfType(TransicaoInvalidaException.class).isThrownBy(ingresso::utilizar);
    }

    @Test
    void naoLiberaReservaDeIngressoDisponivel() {
        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> novoDisponivel().liberarReserva());
    }
}
