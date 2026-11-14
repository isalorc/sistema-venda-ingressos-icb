package br.com.icb.ingressos.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.com.icb.ingressos.ports.out.TokenClientePort;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Implementação do {@link TokenClientePort} com JWT HS256. Usa o mesmo segredo do
 * token de admin ({@code app.jwt.secret}); o claim {@code papel=CLIENTE} separa os
 * dois. O {@code subject} é o id do cliente.
 */
@Component
public class JwtTokenCliente implements TokenClientePort {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenCliente.class);

    private final SecretKey chave;
    private final Duration expiracao;
    private final Clock clock;

    public JwtTokenCliente(@Value("${app.jwt.secret}") String segredo,
                           @Value("${app.cliente.token-expiracao}") Duration expiracao,
                           Clock clock) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracao = expiracao;
        this.clock = clock;
    }

    @Override
    public String gerar(Long usuarioId) {
        var agora = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim(PapelToken.CLAIM, PapelToken.CLIENTE)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracao)))
                .signWith(chave)
                .compact();
    }

    @Override
    public Optional<Long> validar(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(chave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!PapelToken.CLIENTE.equals(claims.get(PapelToken.CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException excecao) {
            log.debug("Token de cliente rejeitado: {}", excecao.getMessage());
            return Optional.empty();
        }
    }
}
