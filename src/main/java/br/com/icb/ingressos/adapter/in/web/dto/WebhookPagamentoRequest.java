package br.com.icb.ingressos.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da notificação recebida do gateway de pagamento (RF-11). O campo
 * {@code status} traz o vocabulário do gateway (ex.: {@code approved},
 * {@code rejected}, {@code pending}); a tradução para o domínio é feita no
 * controller.
 */
public record WebhookPagamentoRequest(

        @NotBlank(message = "A referência da cobrança é obrigatória.")
        String referenciaGateway,

        @NotBlank(message = "O status do pagamento é obrigatório.")
        String status) {
}
