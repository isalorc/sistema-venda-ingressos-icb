package br.com.icb.ingressos.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.TestPropertySource;

import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;

/**
 * Prova do MVP navegável (Épicos 4 a 8): o fluxo completo por HTTP sobre os
 * adapters reais em memória — admin faz login, cria evento e lote, cliente
 * lista/consulta e compra, o webhook aprova e o admin vê o inscrito. Reenvio do
 * webhook é idempotente.
 *
 * <p>Os repositórios em memória são compartilhados no contexto Spring, então o
 * teste identifica seu próprio evento pelo id e nunca assume repositório vazio.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.webhook.secret=teste-e2e")
class FluxoDeCompraViaRestTest {

    @Autowired private TestRestTemplate cliente;
    @Autowired private PagamentoRepositoryPort pagamentoRepository;

    private String tokenAdmin;

    @BeforeEach
    void logarComoAdmin() {
        var resposta = cliente.postForEntity("/api/admin/login", json("""
                { "email": "admin@icb.local", "senha": "admin123" }"""), Map.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        tokenAdmin = (String) resposta.getBody().get("token");
    }

    @Test
    void doCadastroDoEventoAteOInscritoConfirmado() {
        var eventoId = cadastrarEvento();
        var loteId = criarLote(eventoId, 5);

        // O evento aparece na listagem pública (sem token) com o resumo de disponibilidade
        var listagem = cliente.getForEntity("/api/eventos", Map[].class);
        assertThat(listagem.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listagem.getBody())
                .anySatisfy(item -> {
                    assertThat(((Number) item.get("id")).longValue()).isEqualTo(eventoId);
                    assertThat(((Number) item.get("ingressosDisponiveis")).intValue()).isEqualTo(5);
                });

        // Detalhe do evento traz o lote com estoque
        var detalhe = cliente.getForEntity("/api/eventos/" + eventoId, Map.class);
        assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detalhe.getBody().get("nome")).isEqualTo("Congresso de Louvor 2026");

        // Cliente compra um ingresso (rota pública)
        var compra = cliente.postForEntity("/api/compras", json("""
                { "loteId": %d, "nome": "Ana", "email": "ana@exemplo.com",
                  "telefone": "11988887777", "metodoPagamento": "PIX" }""".formatted(loteId)), Map.class);
        assertThat(compra.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(compra.getBody().get("statusPedido")).isEqualTo("PENDENTE");
        assertThat((String) compra.getBody().get("qrCodePix")).isNotBlank();
        var pedidoId = ((Number) compra.getBody().get("pedidoId")).longValue();

        // Estoque decrementado
        var detalhePosCompra = cliente.getForEntity("/api/eventos/" + eventoId, Map.class);
        var lotes = (java.util.List<Map<String, Object>>) detalhePosCompra.getBody().get("lotes");
        assertThat(((Number) lotes.get(0).get("quantidadeDisponivel")).intValue()).isEqualTo(4);

        // A rota de inscritos exige token de admin
        assertThat(cliente.getForEntity("/api/admin/eventos/" + eventoId + "/inscritos", Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Antes do pagamento não há inscritos
        assertThat(inscritos(eventoId).getBody()).isEmpty();

        // Webhook sem o segredo → 401 (autenticação própria do webhook, RF-16)
        assertThat(cliente.postForEntity("/api/webhooks/pagamento",
                json("""
                        { "referenciaGateway": "qualquer", "status": "approved" }"""), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Webhook aprovado com o segredo → 200 e o inscrito aparece
        var referencia = pagamentoRepository.buscarPorPedido(pedidoId).orElseThrow().getReferenciaGateway();
        assertThat(enviarWebhook(referencia, "approved").getStatusCode()).isEqualTo(HttpStatus.OK);

        var inscritos = inscritos(eventoId);
        assertThat(inscritos.getBody()).hasSize(1);
        assertThat(inscritos.getBody()[0].get("nomeCliente")).isEqualTo("Ana");
        assertThat((String) inscritos.getBody()[0].get("codigoQr")).isNotBlank();

        // Reenvio do webhook é idempotente
        assertThat(enviarWebhook(referencia, "approved").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(inscritos(eventoId).getBody()).hasSize(1);
    }

    private long cadastrarEvento() {
        var resposta = adminPost("/api/admin/eventos", """
                { "nome": "Congresso de Louvor 2026", "descricao": "Anual",
                  "dataHora": "2026-12-20T19:00:00" }""");
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resposta.getBody().get("id")).longValue();
    }

    private long criarLote(long eventoId, int quantidade) {
        var resposta = adminPost("/api/admin/eventos/" + eventoId + "/lotes",
                "{ \"nome\": \"Inteira\", \"preco\": 60.00, \"quantidadeTotal\": %d }".formatted(quantidade));
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) resposta.getBody().get("id")).longValue();
    }

    private ResponseEntity<Map> adminPost(String caminho, String corpo) {
        return cliente.exchange(caminho, HttpMethod.POST, new HttpEntity<>(corpo, comToken()), Map.class);
    }

    private ResponseEntity<Map[]> inscritos(long eventoId) {
        return cliente.exchange("/api/admin/eventos/" + eventoId + "/inscritos",
                HttpMethod.GET, new HttpEntity<>(comToken()), Map[].class);
    }

    private ResponseEntity<Map> enviarWebhook(String referencia, String status) {
        var headers = jsonHeaders();
        headers.set("X-Webhook-Token", "teste-e2e");
        var corpo = """
                { "referenciaGateway": "%s", "status": "%s" }""".formatted(referencia, status);
        return cliente.exchange("/api/webhooks/pagamento", HttpMethod.POST,
                new HttpEntity<>(corpo, headers), Map.class);
    }

    private HttpHeaders comToken() {
        var headers = jsonHeaders();
        headers.setBearerAuth(tokenAdmin);
        return headers;
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
