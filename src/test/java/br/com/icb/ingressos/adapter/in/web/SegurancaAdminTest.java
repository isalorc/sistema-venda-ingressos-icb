package br.com.icb.ingressos.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Integração da segurança (Épico 8): só {@code /api/admin/**} (exceto o login)
 * exige o token; login emite o token; CORS responde ao preflight.
 *
 * <p>Usa a credencial de dev do {@code application.yml} (admin@icb.local / admin123).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SegurancaAdminTest {

    @Autowired private TestRestTemplate cliente;

    private static final String EVENTO = """
            { "nome": "Evento Seguro", "dataHora": "2027-05-01T20:00:00" }""";

    @Test
    void rotaAdminSemTokenRetorna401NoFormatoPadrao() {
        var resposta = cliente.postForEntity("/api/admin/eventos", json(EVENTO), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("status", 401);
        assertThat(resposta.getBody()).containsKey("caminho");
    }

    @Test
    void rotaAdminComTokenValidoPassa() {
        var token = login("admin@icb.local", "admin123");

        var headers = jsonHeaders();
        headers.setBearerAuth(token);
        var resposta = cliente.exchange("/api/admin/eventos", HttpMethod.POST,
                new HttpEntity<>(EVENTO, headers), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsKey("id");
    }

    @Test
    void tokenAdulteradoRetorna401() {
        var headers = jsonHeaders();
        headers.setBearerAuth("token.invalido.aqui");
        var resposta = cliente.exchange("/api/admin/eventos", HttpMethod.POST,
                new HttpEntity<>(EVENTO, headers), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginComSenhaErradaRetorna401() {
        var resposta = cliente.postForEntity("/api/admin/login", json("""
                { "email": "admin@icb.local", "senha": "errada" }"""), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("mensagem", "E-mail ou senha inválidos.");
    }

    @Test
    void rotaPublicaSegueSemToken() {
        var resposta = cliente.getForEntity("/api/eventos", Object[].class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void preflightCorsDeOrigemPermitidaRespondeComOsHeaders() {
        var headers = new HttpHeaders();
        headers.setOrigin("http://localhost:5173");
        headers.setAccessControlRequestMethod(HttpMethod.POST);

        var resposta = cliente.exchange("/api/compras", HttpMethod.OPTIONS,
                new HttpEntity<>(headers), String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }

    private String login(String email, String senha) {
        var resposta = cliente.postForEntity("/api/admin/login",
                json("{ \"email\": \"%s\", \"senha\": \"%s\" }".formatted(email, senha)), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) resposta.getBody().get("token");
    }

    private static HttpEntity<String> json(String corpo) {
        return new HttpEntity<>(corpo, jsonHeaders());
    }

    private static HttpHeaders jsonHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
