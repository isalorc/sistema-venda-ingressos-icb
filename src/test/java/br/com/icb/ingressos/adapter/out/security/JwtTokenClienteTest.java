package br.com.icb.ingressos.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class JwtTokenClienteTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-para-hs256";
    private static final Instant AGORA = Instant.parse("2026-08-30T12:00:00Z");
    private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);

    private final JwtTokenCliente tokenCliente = new JwtTokenCliente(SEGREDO, Duration.ofDays(7), RELOGIO);
    private final JwtTokenAdmin tokenAdmin = new JwtTokenAdmin(SEGREDO, Duration.ofHours(2), RELOGIO);

    @Test
    void gerarEValidarDevolveOIdDoCliente() {
        var token = tokenCliente.gerar(42L);

        assertThat(tokenCliente.validar(token)).contains(42L);
    }

    @Test
    void tokenDeClienteNaoValeComoAdmin() {
        var token = tokenCliente.gerar(42L);

        assertThat(tokenAdmin.validar(token)).isEmpty();
    }

    @Test
    void tokenDeAdminNaoValeComoCliente() {
        var token = tokenAdmin.gerar("admin@icb.local");

        assertThat(tokenCliente.validar(token)).isEmpty();
    }

    @Test
    void tokenExpiradoNaoValida() {
        var token = tokenCliente.gerar(42L);
        var oitoDiasDepois = new JwtTokenCliente(SEGREDO, Duration.ofDays(7),
                Clock.fixed(AGORA.plus(Duration.ofDays(8)), ZoneOffset.UTC));

        assertThat(oitoDiasDepois.validar(token)).isEmpty();
    }

    @Test
    void lixoNaoValida() {
        assertThat(tokenCliente.validar("nao-e-um-jwt")).isEmpty();
    }
}
