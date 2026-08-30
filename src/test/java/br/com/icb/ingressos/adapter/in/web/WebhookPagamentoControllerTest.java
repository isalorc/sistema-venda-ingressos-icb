package br.com.icb.ingressos.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.NotificacaoPagamento;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.ResultadoPagamento;

@WebMvcTest(WebhookPagamentoController.class)
@TestPropertySource(properties = "app.webhook.secret=segredo-teste")
class WebhookPagamentoControllerTest {

    private static final String CORPO = """
            { "referenciaGateway": "fake-123", "status": "%s" }""";

    @Autowired private MockMvc mockMvc;

    @MockBean private ConfirmarPagamentoUseCase confirmarPagamento;

    @Test
    void semTokenRetorna401ESemChamarOCasoDeUso() throws Exception {
        mockMvc.perform(post("/api/webhooks/pagamento")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted("approved")))
                .andExpect(status().isUnauthorized());

        verify(confirmarPagamento, never()).confirmar(any());
    }

    @Test
    void tokenErradoRetorna401() throws Exception {
        mockMvc.perform(post("/api/webhooks/pagamento")
                        .header("X-Webhook-Token", "errado")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted("approved")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusAprovadoComTokenValidoConfirmaPagamento() throws Exception {
        mockMvc.perform(post("/api/webhooks/pagamento")
                        .header("X-Webhook-Token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted("approved")))
                .andExpect(status().isOk());

        verify(confirmarPagamento).confirmar(
                new NotificacaoPagamento("fake-123", ResultadoPagamento.APROVADO));
    }

    @Test
    void statusIntermediarioRetorna200SemChamarOCasoDeUso() throws Exception {
        mockMvc.perform(post("/api/webhooks/pagamento")
                        .header("X-Webhook-Token", "segredo-teste")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted("pending")))
                .andExpect(status().isOk());

        verify(confirmarPagamento, never()).confirmar(any());
    }
}
