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

import br.com.icb.ingressos.ports.out.TokenAdminPort;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Implementação do {@link TokenAdminPort} com JWT assinado em HMAC-SHA256
 * (biblioteca jjwt). O segredo vem de {@code app.jwt.secret} (RNF-13) e precisa
 * ter pelo menos 32 bytes (256 bits) para o HS256.
 */
@Component
public class JwtTokenAdmin implements TokenAdminPort {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenAdmin.class);

    private final SecretKey chave;
    private final Duration expiracao;
    private final Clock clock;

    public JwtTokenAdmin(@Value("${app.jwt.secret}") String segredo,
                         @Value("${app.jwt.expiracao}") Duration expiracao,
                         Clock clock) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracao = expiracao;
        this.clock = clock;
    }

    @Override
    public String gerar(String emailAdmin) {
        var agora = clock.instant();
        return Jwts.builder()
                .subject(emailAdmin)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracao)))
                .signWith(chave)
                .compact();
    }

    @Override
    public Optional<String> validar(String token) {
        try {
            var email = Jwts.parser()
                    .verifyWith(chave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(email);
        } catch (JwtException | IllegalArgumentException excecao) {
            log.debug("Token de admin rejeitado: {}", excecao.getMessage());
            return Optional.empty();
        }
    }
}
