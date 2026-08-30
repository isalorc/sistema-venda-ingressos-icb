package br.com.icb.ingressos.domain.enums;

/**
 * Estados de um {@code Pagamento}. Mapeável para os status do Mercado Pago
 * ({@code approved}, {@code rejected}, {@code pending}, {@code cancelled}).
 *
 * <pre>
 * PENDENTE --aprovar--> APROVADO
 * PENDENTE --recusar--> RECUSADO
 * PENDENTE --cancelar--> CANCELADO
 * </pre>
 */
public enum StatusPagamento {
    PENDENTE,
    APROVADO,
    RECUSADO,
    CANCELADO
}
