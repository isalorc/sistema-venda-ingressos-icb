package br.com.icb.ingressos.adapter.in.web;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.icb.ingressos.ports.out.TokenAdminPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê o token do header {@code Authorization: Bearer <token>} e, se válido,
 * autentica a requisição como administrador (papel {@code ROLE_ADMIN}).
 *
 * <p>Sem token ou com token inválido a requisição segue não autenticada — quem
 * decide se isso é 401 é a regra de autorização do {@code SecurityConfig}.
 */
public class JwtAdminFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";
    private static final List<SimpleGrantedAuthority> PAPEL_ADMIN =
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));

    private final TokenAdminPort tokenAdmin;

    public JwtAdminFilter(TokenAdminPort tokenAdmin) {
        this.tokenAdmin = tokenAdmin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao,
                                    HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        var header = requisicao.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIXO)) {
            tokenAdmin.validar(header.substring(PREFIXO.length()).trim())
                    .ifPresent(this::autenticar);
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(String emailAdmin) {
        var autenticacao = new UsernamePasswordAuthenticationToken(emailAdmin, null, PAPEL_ADMIN);
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}
