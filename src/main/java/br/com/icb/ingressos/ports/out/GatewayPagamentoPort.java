package br.com.icb.ingressos.ports.out;

import java.math.BigDecimal;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;

/**
 * Porta de saída para o gateway de pagamento externo (RF-09).
 *
 * <p>No MVP a implementação é um adapter <em>fake</em>; a integração real com o
 * Mercado Pago (sandbox) é etapa posterior. A confirmação do resultado do
 * pagamento não passa por aqui: chega de forma assíncrona pelo webhook.
 */
public interface GatewayPagamentoPort {

    /**
     * Solicita ao gateway a criação de uma cobrança para o pedido e devolve os
     * dados que o cliente usa para pagar (link e/ou QR Code PIX).
     */
    CobrancaCriada criarCobranca(DadosCobranca dados);

    /**
     * Dados enviados ao gateway para gerar a cobrança.
     */
    record DadosCobranca(
            Long pedidoId,
            BigDecimal valor,
            MetodoPagamento metodo,
            String nomeCliente,
            String emailCliente) {
    }

    /**
     * Retorno do gateway após criar a cobrança.
     *
     * @param referenciaGateway identificador da cobrança no gateway, guardado no
     *                          {@code Pagamento} para correlacionar o webhook
     * @param urlPagamento      link de checkout/pagamento (pode ser nulo quando
     *                          só há QR PIX)
     * @param qrCodePix         payload do QR Code PIX (pode ser nulo para cartão)
     */
    record CobrancaCriada(
            String referenciaGateway,
            String urlPagamento,
            String qrCodePix) {
    }
}
