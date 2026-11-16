package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.in.ConsultarPedidosClienteUseCase.EventoDoPedido;
import br.com.icb.ingressos.ports.in.ConsultarPedidosClienteUseCase.PedidoResumo;

/**
 * Item de "Meus pedidos". {@code evento} é nulo só em pedidos antigos cujo lote não
 * pôde ser recuperado; {@code expiraEm} só vem em pedidos {@code PENDENTE}.
 */
public record PedidoClienteResponse(
        Long id,
        LocalDateTime dataPedido,
        StatusPedido status,
        BigDecimal valorTotal,
        int quantidadeIngressos,
        Evento evento,
        LocalDateTime expiraEm) {

    public record Evento(
            Long id,
            String nome,
            LocalDateTime dataHora,
            LocalDateTime dataFim,
            String imagemUrl,
            Long loteId,
            String nomeLote) {

        static Evento de(EventoDoPedido evento) {
            return evento == null ? null : new Evento(evento.id(), evento.nome(), evento.dataHora(),
                    evento.dataFim(), evento.imagemUrl(), evento.loteId(), evento.nomeLote());
        }
    }

    public static PedidoClienteResponse de(PedidoResumo pedido) {
        return new PedidoClienteResponse(pedido.id(), pedido.dataPedido(), pedido.status(),
                pedido.valorTotal(), pedido.quantidadeIngressos(), Evento.de(pedido.evento()),
                pedido.expiraEm());
    }
}
