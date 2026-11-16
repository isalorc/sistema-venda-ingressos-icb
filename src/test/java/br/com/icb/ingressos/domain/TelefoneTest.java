package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TelefoneTest {

    @ParameterizedTest
    @CsvSource({
            "13999998888, 13999998888",
            "'(13) 99999-8888', 13999998888",
            "'13 99999 8888', 13999998888",
            "'+55 13 99999-8888', 13999998888",
            "5513999998888, 13999998888",
            "'(11) 3333-4444', 1133334444",
            "551133334444, 1133334444",
    })
    void aceitaCelularEFixoComOuSemMascaraEGuardaSoDigitos(String entrada, String esperado) {
        assertThat(Telefone.normalizar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "999998888",       // sem DDD
            "1399999888",      // celular com 8 dígitos começando por 9
            "13899998888",     // 11 dígitos sem o 9 do celular
            "0139999-8888",    // DDD com zero
            "(10) 99999-8888", // DDD inválido
            "1313333444",      // fixo começando por 1
            "abc",
            "139999988881",    // dígitos demais
    })
    void recusaTelefonesInvalidos(String entrada) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Telefone.normalizar(entrada))
                .withMessageContaining("Telefone inválido");
    }
}
