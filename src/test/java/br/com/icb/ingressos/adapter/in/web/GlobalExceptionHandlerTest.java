package br.com.icb.ingressos.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GlobalExceptionHandlerTest.ControllerDeTeste.class)
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

    @Test
    void recursoNaoEncontradoNoDominioRetorna404() {
        var resposta = cliente.getForEntity("/__teste/erros/nao-encontrado", ErroResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().mensagem()).contains("Lote");
    }

    @Test
    void ingressoEsgotadoRetorna409() {
        var resposta = cliente.getForEntity("/__teste/erros/esgotado", ErroResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().status()).isEqualTo(409);
    }

    @Test
    void transicaoInvalidaRetorna409() {
        var resposta = cliente.getForEntity("/__teste/erros/transicao", ErroResponse.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @TestConfiguration
    @RestController
    @RequestMapping("/__teste/erros")
    static class ControllerDeTeste {

        @GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new RecursoNaoEncontradoException("Lote", 99L);
        }

        @GetMapping("/esgotado")
        void esgotado() {
            throw new IngressoEsgotadoException(1L);
        }

        @GetMapping("/transicao")
        void transicao() {
            throw new TransicaoInvalidaException("Pedido não pode ser pago no status EXPIRADO.");
        }
    }
}
