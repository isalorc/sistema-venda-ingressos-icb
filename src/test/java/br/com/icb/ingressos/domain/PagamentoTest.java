package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;

class PagamentoTest {

    private static Pagamento pendente() {
        return Pagamento.pendente(1L, new BigDecimal("50.00"), MetodoPagamento.PIX);
    }

    @Test
    void pagamentoNovoNascePendenteSemData() {
        var pagamento = pendente();

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(pagamento.getDataPagamento()).isNull();
    }

    @Test
    void aprovarRegistraStatusEData() {
        var pagamento = pendente();
        var agora = LocalDateTime.of(2026, 8, 29, 10, 5);

        pagamento.aprovar(agora);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.getDataPagamento()).isEqualTo(agora);
    }

    @Test
    void recusarECancelarSaemDePendente() {
        var recusado = pendente();
        recusado.recusar();
        assertThat(recusado.getStatus()).isEqualTo(StatusPagamento.RECUSADO);

        var cancelado = pendente();
        cancelado.cancelar();
        assertThat(cancelado.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
    }

    @Test
    void aprovarExigeData() {
        assertThatIllegalArgumentException().isThrownBy(() -> pendente().aprovar(null));
    }

    @Test
    void reaprovarPagamentoLancaTransicaoInvalida() {
        var pagamento = pendente();
        pagamento.aprovar(LocalDateTime.of(2026, 8, 29, 10, 5));

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> pagamento.aprovar(LocalDateTime.of(2026, 8, 29, 10, 6)));
    }

    @Test
    void naoRecusaPagamentoJaAprovado() {
        var pagamento = pendente();
        pagamento.aprovar(LocalDateTime.of(2026, 8, 29, 10, 5));

        assertThatExceptionOfType(TransicaoInvalidaException.class).isThrownBy(pagamento::recusar);
    }

    @Test
    void vincularCobrancaGuardaAReferenciaDoGateway() {
        var pagamento = pendente();

        pagamento.vincularCobranca("mp-cobranca-123");

        assertThat(pagamento.getReferenciaGateway()).isEqualTo("mp-cobranca-123");
    }

    @Test
    void vincularCobrancaExigeReferenciaNaoVazia() {
        assertThatIllegalArgumentException().isThrownBy(() -> pendente().vincularCobranca("  "));
    }

    @Test
    void naoRevinculaCobrancaJaVinculada() {
        var pagamento = pendente();
        pagamento.vincularCobranca("mp-cobranca-123");

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> pagamento.vincularCobranca("mp-cobranca-456"));
    }

    @Test
    void naoVinculaCobrancaEmPagamentoJaAprovado() {
        var pagamento = pendente();
        pagamento.aprovar(LocalDateTime.of(2026, 8, 29, 10, 5));

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> pagamento.vincularCobranca("mp-cobranca-123"));
    }
}
