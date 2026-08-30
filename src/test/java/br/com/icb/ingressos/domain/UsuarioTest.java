package br.com.icb.ingressos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 29, 10, 0);

    @Test
    void normalizaEmailParaMinusculasParaServirDeChaveNatural() {
        var usuario = Usuario.novo("Ana", "Ana.Lorena@Email.COM", "11999998888", AGORA);

        assertThat(usuario.getEmail()).isEqualTo("ana.lorena@email.com");
    }

    @Test
    void rejeitaEmailSemFormatoValido() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Usuario.novo("Ana", "ana(at)email", "11999998888", AGORA));
    }

    @Test
    void rejeitaNomeEmBranco() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Usuario.novo("  ", "ana@email.com", "11999998888", AGORA));
    }

    @Test
    void rejeitaTelefoneAusente() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Usuario.novo("Ana", "ana@email.com", null, AGORA));
    }
}
