package br.com.icb.ingressos.adapter.in.web.dto;

import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.ConsultarInscritosUseCase.Inscrito;

/**
 * Inscrito de um evento: cliente com pedido pago e o ingresso vendido (RF-25).
 */
public record InscritoResponse(
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        Long loteId,
        String nomeLote,
        Long pedidoId,
        String codigoQr,
        LocalDateTime dataCompra) {

    public static InscritoResponse de(Inscrito inscrito) {
        return new InscritoResponse(
                inscrito.nomeCliente(),
                inscrito.emailCliente(),
                inscrito.telefoneCliente(),
                inscrito.loteId(),
                inscrito.nomeLote(),
                inscrito.pedidoId(),
                inscrito.codigoQr(),
                inscrito.dataCompra());
    }
}
