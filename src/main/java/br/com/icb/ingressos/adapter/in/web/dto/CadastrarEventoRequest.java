package br.com.icb.ingressos.adapter.in.web.dto;

import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.CadastrarEventoUseCase.CadastrarEventoCommand;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da requisição de cadastro de evento (RF-20). Operação administrativa.
 */
public record CadastrarEventoRequest(

        @NotBlank(message = "O nome do evento é obrigatório.")
        String nome,

        String descricao,

        @NotNull(message = "A data e hora do evento são obrigatórias.")
        @Future(message = "A data e hora do evento devem estar no futuro.")
        LocalDateTime dataHora) {

    public CadastrarEventoCommand toCommand() {
        return new CadastrarEventoCommand(nome, descricao, dataHora);
    }
}
