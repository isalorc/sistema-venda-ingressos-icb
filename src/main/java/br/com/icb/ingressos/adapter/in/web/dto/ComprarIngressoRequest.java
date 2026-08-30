package br.com.icb.ingressos.adapter.in.web.dto;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ComprarIngressoCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisição de compra de ingresso (RF-04). O cliente não tem cadastro:
 * informa nome, e-mail e telefone na própria compra (RN-4).
 */
public record ComprarIngressoRequest(

        @NotNull(message = "O lote é obrigatório.")
        Long loteId,

        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "O telefone é obrigatório.")
        String telefone,

        @NotNull(message = "O método de pagamento é obrigatório.")
        MetodoPagamento metodoPagamento) {

    public ComprarIngressoCommand toCommand() {
        return new ComprarIngressoCommand(loteId, nome, email, telefone, metodoPagamento);
    }
}
