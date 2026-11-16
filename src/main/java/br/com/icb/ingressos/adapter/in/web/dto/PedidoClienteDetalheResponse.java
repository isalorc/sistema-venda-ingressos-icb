package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.in.ConsultarPedidosClienteUseCase.PedidoDetalhe;

/**
 * Detalhe de um pedido do cliente: os campos do resumo, o pagamento e os
 * ingressos. {@code codigoQr} de cada ingresso só vem depois do pagamento
 * aprovado; {@code ingressos} vem vazio se a reserva expirou ou foi recusada.
 */
public record PedidoClienteDetalheResponse(
        Long id,
        LocalDateTime dataPedido,
        StatusPedido status,
        BigDecimal valorTotal,
        int quantidadeIngressos,
        PedidoClienteResponse.Evento evento,
        LocalDateTime expiraEm,
        MetodoPagamento metodoPagamento,
        StatusPagamento statusPagamento,
        List<Ingresso> ingressos) {

    public record Ingresso(Long id, String nomeParticipante, StatusIngresso status, String codigoQr) {
    }

    public static PedidoClienteDetalheResponse de(PedidoDetalhe detalhe) {
        var resumo = PedidoClienteResponse.de(detalhe.pedido());
        return new PedidoClienteDetalheResponse(
                resumo.id(), resumo.dataPedido(), resumo.status(), resumo.valorTotal(),
                resumo.quantidadeIngressos(), resumo.evento(), resumo.expiraEm(),
                detalhe.metodoPagamento(), detalhe.statusPagamento(),
                detalhe.ingressos().stream()
                        .map(i -> new Ingresso(i.id(), i.nomeParticipante(), i.status(), i.codigoQr()))
                        .toList());
    }
}
