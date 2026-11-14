package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;

/**
 * Caso de uso: "Meus pedidos" — o cliente logado acompanha os próprios pedidos e
 * vê os ingressos (com QR) dos pedidos pagos. Pedido de outro cliente responde
 * como inexistente.
 */
public interface ConsultarPedidosClienteUseCase {

    /** Pedidos do cliente, mais recentes primeiro. */
    List<PedidoResumo> listar(Long usuarioId);

    PedidoDetalhe detalhar(Long usuarioId, Long pedidoId);

    /**
     * @param evento    nulo só em pedidos antigos cujo lote não pôde ser recuperado
     * @param expiraEm  quando a reserva expira; só em pedidos {@code PENDENTE}
     */
    record PedidoResumo(
            Long id,
            LocalDateTime dataPedido,
            StatusPedido status,
            BigDecimal valorTotal,
            int quantidadeIngressos,
            EventoDoPedido evento,
            LocalDateTime expiraEm) {
    }

    record EventoDoPedido(
            Long id,
            String nome,
            LocalDateTime dataHora,
            LocalDateTime dataFim,
            String imagemUrl,
            Long loteId,
            String nomeLote) {
    }

    /**
     * @param ingressos vazio quando a reserva expirou ou o pagamento foi recusado
     *                  (os ingressos voltaram ao estoque)
     */
    record PedidoDetalhe(
            PedidoResumo pedido,
            MetodoPagamento metodoPagamento,
            StatusPagamento statusPagamento,
            List<IngressoDoPedido> ingressos) {
    }

    /** @param codigoQr só depois do pagamento aprovado (RN-6) */
    record IngressoDoPedido(
            Long id,
            String nomeParticipante,
            StatusIngresso status,
            String codigoQr) {
    }
}
