package br.com.icb.ingressos.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class JwtTokenAdminTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-para-hs256";
    private static final String EMAIL = "admin@icb.local";
    private static final Instant AGORA = Instant.parse("2026-08-30T12:00:00Z");

    private static JwtTokenAdmin comRelogio(Instant instante) {
        return new JwtTokenAdmin(SEGREDO, Duration.ofHours(2),
                Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void gerarEValidarFazUmCicloCompleto() {
        var jwt = comRelogio(AGORA);

        var token = jwt.gerar(EMAIL);

        assertThat(jwt.validar(token)).contains(EMAIL);
    }

    @Test
    void tokenExpiradoNaoValida() {
        var emissor = comRelogio(AGORA.minus(Duration.ofHours(3)));
        var token = emissor.gerar(EMAIL);

        var verificadorNoPresente = comRelogio(AGORA);

        assertThat(verificadorNoPresente.validar(token)).isEmpty();
    }

    @Test
    void tokenComOutraAssinaturaNaoValida() {
        var token = comRelogio(AGORA).gerar(EMAIL);
        var outroEmissor = new JwtTokenAdmin("outro-segredo-com-mais-de-32-bytes-aqui-ok", Duration.ofHours(2),
                Clock.fixed(AGORA, ZoneOffset.UTC));

        assertThat(outroEmissor.validar(token)).isEmpty();
    }

    @Test
    void lixoNaoValida() {
        assertThat(comRelogio(AGORA).validar("nao-e-um-jwt")).isEmpty();
    }
}
