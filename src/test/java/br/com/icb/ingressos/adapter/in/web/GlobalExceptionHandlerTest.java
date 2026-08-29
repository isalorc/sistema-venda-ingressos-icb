package br.com.icb.ingressos.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalExceptionHandlerTest {

    @Autowired
    private TestRestTemplate cliente;

    @Test
    void rotaInexistenteRetorna404NoFormatoPadrao() {
        var resposta = cliente.getForEntity("/rota-que-nao-existe", ErroResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().status()).isEqualTo(404);
        assertThat(resposta.getBody().caminho()).isEqualTo("/rota-que-nao-existe");
        assertThat(resposta.getBody().mensagem()).isEqualTo("Recurso não encontrado.");
        assertThat(resposta.getBody().timestamp()).isNotNull();
    }

    @Test
    void metodoNaoSuportadoRetorna405NoFormatoPadrao() {
        var resposta = cliente.postForEntity("/actuator/health", null, ErroResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().status()).isEqualTo(405);
        assertThat(resposta.getBody().mensagem()).isEqualTo("Método HTTP não suportado para este recurso.");
    }
}
