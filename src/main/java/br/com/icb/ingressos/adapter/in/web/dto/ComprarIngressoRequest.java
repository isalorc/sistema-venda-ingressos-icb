package br.com.icb.ingressos.adapter.in.web.dto;

import java.util.List;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ComprarIngressoCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da compra (RF-04). O comprador vem do token de cliente; o corpo traz o
 * lote e um nome por ingresso (RN-1). O limite por pedido é validado no caso de uso.
 */
public record ComprarIngressoRequest(
        @NotNull(message = "O lote é obrigatório.")
        Long loteId,

        @NotEmpty(message = "Informe pelo menos um participante.")
        List<@NotBlank(message = "Informe o nome de cada participante.") String> participantes,

        @NotNull(message = "O método de pagamento é obrigatório.")
        MetodoPagamento metodoPagamento) {

    public ComprarIngressoCommand toCommand(Long usuarioId) {
        return new ComprarIngressoCommand(usuarioId, loteId, participantes, metodoPagamento);
    }
}
