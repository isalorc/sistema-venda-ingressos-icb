package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;

/**
 * Caso de uso: comprar um ingresso (RF-04 a RF-10).
 *
 * <p>Fluxo: resolve/cria o cliente pelo e-mail (RN-4), reserva sob bloqueio
 * pessimista um ingresso disponível do lote (RN-1, RNF-07), cria {@code Pedido}
 * e {@code Pagamento} pendentes com valor igual ao preço do lote e solicita a
 * cobrança ao gateway. Se o lote estiver esgotado, nada é criado (RF-07).
 */
public interface ComprarIngressoUseCase {

    ResultadoCompra comprar(ComprarIngressoCommand comando);

    record ComprarIngressoCommand(
            Long loteId,
            String nomeCliente,
            String emailCliente,
            String telefoneCliente,
            MetodoPagamento metodoPagamento) {
    }

    /**
     * Dados devolvidos ao cliente após iniciar a compra.
     *
     * @param urlPagamento link de checkout (pode ser nulo quando só há QR PIX)
     * @param qrCodePix    payload do QR Code PIX (pode ser nulo para cartão)
     */
    record ResultadoCompra(
            Long pedidoId,
            Long ingressoId,
            BigDecimal valorTotal,
            StatusPedido statusPedido,
            String urlPagamento,
            String qrCodePix) {
    }
}
