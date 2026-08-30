package br.com.icb.ingressos.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.WebhookPagamentoRequest;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.NotificacaoPagamento;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.ResultadoPagamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Webhook de confirmação de pagamento (RF-11 a RF-16).
 *
 * <p>Autentica a notificação pelo segredo compartilhado no header
 * {@code X-Webhook-Token} (RF-16) e traduz o status do vocabulário do gateway
 * para o domínio. Estados intermediários (ex.: {@code pending}) são ignorados com
 * resposta 200. O processamento em si é idempotente no caso de uso (RF-12), então
 * reentregas são inofensivas.
 */
@RestController
@RequestMapping("/api/webhooks/pagamento")
@Tag(name = "Webhook", description = "Notificação de resultado de pagamento pelo gateway")
public class WebhookPagamentoController {

    private static final Logger log = LoggerFactory.getLogger(WebhookPagamentoController.class);

    /** Vocabulário do gateway → resultado no domínio. Ausente = estado intermediário, ignorar. */
    private static final Map<String, ResultadoPagamento> RESULTADO_POR_STATUS = Map.of(
            "approved", ResultadoPagamento.APROVADO,
            "aprovado", ResultadoPagamento.APROVADO,
            "rejected", ResultadoPagamento.RECUSADO,
            "cancelled", ResultadoPagamento.RECUSADO,
            "canceled", ResultadoPagamento.RECUSADO,
            "recusado", ResultadoPagamento.RECUSADO);

    private final ConfirmarPagamentoUseCase confirmarPagamento;
    private final byte[] segredoEsperado;

    public WebhookPagamentoController(ConfirmarPagamentoUseCase confirmarPagamento,
                                      @Value("${app.webhook.secret}") String segredoEsperado) {
        this.confirmarPagamento = confirmarPagamento;
        this.segredoEsperado = segredoEsperado.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    @Operation(summary = "Recebe a notificação de pagamento do gateway")
    public ResponseEntity<Void> receber(
            @RequestHeader(name = "X-Webhook-Token", required = false) String token,
            @Valid @RequestBody WebhookPagamentoRequest requisicao) {

        autenticar(token);

        var resultado = traduzir(requisicao.status());
        if (resultado.isEmpty()) {
            log.info("Webhook ignorado: status intermediário '{}' para a cobrança {}.",
                    requisicao.status(), requisicao.referenciaGateway());
            return ResponseEntity.ok().build();
        }

        confirmarPagamento.confirmar(new NotificacaoPagamento(requisicao.referenciaGateway(), resultado.get()));
        return ResponseEntity.ok().build();
    }

    private void autenticar(String token) {
        var recebido = (token == null ? "" : token).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(segredoEsperado, recebido)) {
            throw new AutenticacaoWebhookException();
        }
    }

    private static Optional<ResultadoPagamento> traduzir(String status) {
        return Optional.ofNullable(RESULTADO_POR_STATUS.get(status.strip().toLowerCase(Locale.ROOT)));
    }
}
