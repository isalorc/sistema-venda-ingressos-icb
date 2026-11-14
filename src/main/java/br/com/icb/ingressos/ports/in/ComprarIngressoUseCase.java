package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.util.List;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;

/**
 * Caso de uso: comprar ingressos (RF-04 a RF-10).
 *
 * <p>Fluxo: o cliente logado escolhe o lote e informa um nome por ingresso (RN-1,
 * de 1 até o limite por pedido). Sob bloqueio pessimista do lote (RNF-07) o
 * sistema reserva um ingresso disponível por participante, cria {@code Pedido} e
 * {@code Pagamento} pendentes com valor = preço do lote × quantidade e solicita a
 * cobrança ao gateway. Se não houver ingressos suficientes, nada é criado.
 */
public interface ComprarIngressoUseCase {

    ResultadoCompra comprar(ComprarIngressoCommand comando);

    /**
     * @param usuarioId     cliente autenticado (vem do token, nunca do corpo)
     * @param participantes um nome por ingresso
     */
    record ComprarIngressoCommand(
            Long usuarioId,
            Long loteId,
            List<String> participantes,
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
            List<Long> ingressoIds,
            BigDecimal valorTotal,
            StatusPedido statusPedido,
            String urlPagamento,
            String qrCodePix) {
    }
}
