package br.com.icb.ingressos.ports.in;

/**
 * Caso de uso: processar a notificação de resultado de pagamento vinda do
 * webhook do gateway (RF-11 a RF-14).
 *
 * <p>O processamento é <strong>idempotente</strong> (RF-12, RNF-09): a mesma
 * notificação pode chegar mais de uma vez e fora de ordem, sem gerar efeito
 * duplicado. A autenticidade da notificação (assinatura/segredo — RF-16) é
 * verificada no adapter de entrada, antes de chamar este caso de uso.
 *
 * <ul>
 *   <li><b>Aprovado:</b> {@code Pagamento} → APROVADO, {@code Pedido} → PAGO,
 *       {@code Ingresso} → VENDIDO com código QR gerado (RN-6); notifica o cliente.</li>
 *   <li><b>Recusado:</b> libera a reserva do ingresso, repõe o estoque do lote e
 *       cancela o pagamento.</li>
 * </ul>
 */
public interface ConfirmarPagamentoUseCase {

    void confirmar(NotificacaoPagamento notificacao);

    /**
     * @param referenciaGateway id da cobrança no gateway, usado para localizar o
     *                          {@code Pagamento} correspondente
     */
    record NotificacaoPagamento(
            String referenciaGateway,
            ResultadoPagamento resultado) {
    }

    /**
     * Resultado já traduzido do vocabulário do gateway para o domínio. Estados
     * intermediários do gateway (ex.: {@code pending}) não chegam aqui: o adapter
     * os ignora.
     */
    enum ResultadoPagamento {
        APROVADO,
        RECUSADO
    }
}
