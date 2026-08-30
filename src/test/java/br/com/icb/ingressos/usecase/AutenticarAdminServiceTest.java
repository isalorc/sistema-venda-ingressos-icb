package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase.Credenciais;
import br.com.icb.ingressos.ports.out.TokenAdminPort;

@ExtendWith(MockitoExtension.class)
class AutenticarAdminServiceTest {

    private static final String EMAIL = "admin@icb.local";
    private static final String HASH = "$2a$10$hashfake";

    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenAdminPort tokenAdmin;

    private AutenticarAdminService service;

    @BeforeEach
    void setUp() {
        service = new AutenticarAdminService(passwordEncoder, tokenAdmin, EMAIL, HASH, Duration.ofHours(2));
    }

    @Test
    void credencialCorretaEmiteOToken() {
        when(passwordEncoder.matches("segredo", HASH)).thenReturn(true);
        when(tokenAdmin.gerar(EMAIL)).thenReturn("jwt-abc");

        var emitido = service.autenticar(new Credenciais("Admin@ICB.local", "segredo"));

        assertThat(emitido.token()).isEqualTo("jwt-abc");
        assertThat(emitido.tipo()).isEqualTo("Bearer");
        assertThat(emitido.expiraEmSegundos()).isEqualTo(7200);
    }

    @Test
    void senhaErradaLancaCredenciaisInvalidasSemGerarToken() {
        when(passwordEncoder.matches("errada", HASH)).thenReturn(false);

        assertThatExceptionOfType(CredenciaisInvalidasException.class)
                .isThrownBy(() -> service.autenticar(new Credenciais(EMAIL, "errada")));

        verify(tokenAdmin, never()).gerar(any());
    }

    @Test
    void emailErradoAindaVerificaOHashParaNaoVazarPorTiming() {
        when(passwordEncoder.matches(eq("qualquer"), any())).thenReturn(true);

        assertThatExceptionOfType(CredenciaisInvalidasException.class)
                .isThrownBy(() -> service.autenticar(new Credenciais("outro@exemplo.com", "qualquer")));

        verify(passwordEncoder).matches("qualquer", HASH);
        verify(tokenAdmin, never()).gerar(any());
    }
}
