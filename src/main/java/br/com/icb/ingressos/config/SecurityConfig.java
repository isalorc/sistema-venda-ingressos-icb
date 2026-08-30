package br.com.icb.ingressos.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.icb.ingressos.adapter.in.web.ErroResponse;
import br.com.icb.ingressos.adapter.in.web.JwtAdminFilter;
import br.com.icb.ingressos.ports.out.TokenAdminPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Segurança das rotas administrativas (RF-26 / RNF-12) e CORS (RNF-16).
 *
 * <p>API stateless: sem sessão, sem CSRF. Só {@code /api/admin/**} (exceto o
 * login) exige token; o resto — consulta de eventos, compra, webhook, health,
 * Swagger — segue público. As respostas 401/403 saem no mesmo formato
 * {@link ErroResponse} do resto da API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    TokenAdminPort tokenAdmin,
                                    ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .addFilterBefore(new JwtAdminFilter(tokenAdmin), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(tratamento -> tratamento
                        .authenticationEntryPoint(entryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origensPermitidas) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        var fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }

    private static AuthenticationEntryPoint entryPoint(ObjectMapper objectMapper) {
        return (requisicao, resposta, excecao) ->
                escrever(resposta, objectMapper, HttpStatus.UNAUTHORIZED,
                        "Autenticação obrigatória para esta rota.", requisicao);
    }

    private static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (requisicao, resposta, excecao) ->
                escrever(resposta, objectMapper, HttpStatus.FORBIDDEN,
                        "Acesso negado para esta rota.", requisicao);
    }

    private static void escrever(HttpServletResponse resposta, ObjectMapper objectMapper,
                                 HttpStatus status, String mensagem, HttpServletRequest requisicao) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        var corpo = ErroResponse.de(status, mensagem, requisicao.getRequestURI());
        objectMapper.writeValue(resposta.getWriter(), corpo);
    }
}
