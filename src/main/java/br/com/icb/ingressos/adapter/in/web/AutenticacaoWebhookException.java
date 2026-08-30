package br.com.icb.ingressos.adapter.in.web;

/**
 * Lançada quando a notificação recebida no webhook de pagamento não apresenta um
 * segredo válido (RF-16). Mapeada para HTTP 401 no {@link GlobalExceptionHandler}.
 */
public class AutenticacaoWebhookException extends RuntimeException {

    public AutenticacaoWebhookException() {
        super("Notificação de pagamento não autenticada.");
    }
}
