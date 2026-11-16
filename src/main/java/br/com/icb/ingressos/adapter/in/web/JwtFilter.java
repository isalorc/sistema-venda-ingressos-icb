package br.com.icb.ingressos.adapter.in.web;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.icb.ingressos.ports.out.TokenAdminPort;
import br.com.icb.ingressos.ports.out.TokenClientePort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê o {@code Authorization: Bearer <token>} e autentica a requisição com o papel
 * do token: {@code ROLE_ADMIN} (principal = e-mail do admin) ou
 * {@code ROLE_CLIENTE} (principal = id do cliente, {@link Long}). Token ausente ou
 * inválido segue sem autenticação; quem barra é o {@code SecurityConfig}.
 */
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";
    private static final List<SimpleGrantedAuthority> PAPEL_ADMIN =
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final List<SimpleGrantedAuthority> PAPEL_CLIENTE =
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));

    private final TokenAdminPort tokenAdmin;
    private final TokenClientePort tokenCliente;

    public JwtFilter(TokenAdminPort tokenAdmin, TokenClientePort tokenCliente) {
        this.tokenAdmin = tokenAdmin;
        this.tokenCliente = tokenCliente;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao,
                                    HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        var header = requisicao.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIXO)) {
            var token = header.substring(PREFIXO.length()).trim();
            tokenAdmin.validar(token).ifPresentOrElse(
                    email -> autenticar(email, PAPEL_ADMIN),
                    () -> tokenCliente.validar(token).ifPresent(id -> autenticar(id, PAPEL_CLIENTE)));
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private static void autenticar(Object principal, List<SimpleGrantedAuthority> papeis) {
        var autenticacao = new UsernamePasswordAuthenticationToken(principal, null, papeis);
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}
