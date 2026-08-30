package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;

class PedidoTest {

    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 8, 29, 10, 0);
    private static final Duration TTL = Duration.ofMinutes(15);

    private static Pedido pendente() {
        return Pedido.novo(1L, CRIACAO, new BigDecimal("50.00"));
    }

    @Test
    void pedidoNovoNascePendente() {
        assertThat(pendente().getStatus()).isEqualTo(StatusPedido.PENDENTE);
    }

    @Test
    void marcarPagoCancelarExpirarSaemDePendente() {
        assertThat(transicao(Pedido::marcarPago)).isEqualTo(StatusPedido.PAGO);
        assertThat(transicao(Pedido::cancelar)).isEqualTo(StatusPedido.CANCELADO);
        assertThat(transicao(Pedido::expirar)).isEqualTo(StatusPedido.EXPIRADO);
    }

    @Test
    void naoPagaPedidoJaCancelado() {
        var pedido = pendente();
        pedido.cancelar();

        assertThatExceptionOfType(TransicaoInvalidaException.class).isThrownBy(pedido::marcarPago);
    }

    @Test
    void naoExpiraPedidoJaPago() {
        var pedido = pendente();
        pedido.marcarPago();

        assertThatExceptionOfType(TransicaoInvalidaException.class).isThrownBy(pedido::expirar);
    }

    @Test
    void reservaExpiradaSomenteAposOTtlEEnquantoPendente() {
        var pedido = pendente();

        assertThat(pedido.reservaExpirada(CRIACAO.plusMinutes(14), TTL)).isFalse();
        assertThat(pedido.reservaExpirada(CRIACAO.plusMinutes(15), TTL)).isTrue();
        assertThat(pedido.reservaExpirada(CRIACAO.plusMinutes(30), TTL)).isTrue();
    }

    @Test
    void pedidoPagoNuncaContaComoReservaExpirada() {
        var pedido = pendente();
        pedido.marcarPago();

        assertThat(pedido.reservaExpirada(CRIACAO.plusHours(1), TTL)).isFalse();
    }

    private static StatusPedido transicao(java.util.function.Consumer<Pedido> acao) {
        var pedido = pendente();
        acao.accept(pedido);
        return pedido.getStatus();
    }
}
