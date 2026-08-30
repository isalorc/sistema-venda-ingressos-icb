package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;

import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ResultadoCompra;

/**
 * Dados devolvidos ao cliente após iniciar a compra (RF-09). {@code urlPagamento}
 * e {@code qrCodePix} podem vir nulos conforme o método de pagamento.
 */
public record CompraResponse(
        Long pedidoId,
        Long ingressoId,
        BigDecimal valorTotal,
        StatusPedido statusPedido,
        String urlPagamento,
        String qrCodePix) {

    public static CompraResponse de(ResultadoCompra resultado) {
        return new CompraResponse(
                resultado.pedidoId(),
                resultado.ingressoId(),
                resultado.valorTotal(),
                resultado.statusPedido(),
                resultado.urlPagamento(),
                resultado.qrCodePix());
    }
}
