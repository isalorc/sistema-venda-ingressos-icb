package br.com.icb.ingressos.adapter.in.web.dto;

import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.GerenciarEventoUseCase.DadosDoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do `PUT /api/admin/eventos/{id}` — substituição dos campos editáveis do
 * evento. Envie todos; um opcional ausente ou `null` fica `null`.
 */
public record EditarEventoRequest(

        @NotBlank(message = "O nome do evento é obrigatório.")
        String nome,

        String descricao,

        @NotNull(message = "A data de início do evento é obrigatória.")
        LocalDateTime dataHora,

        LocalDateTime dataFim,

        String imagemUrl) {

    public DadosDoEvento toDados() {
        return new DadosDoEvento(nome, descricao, dataHora, dataFim, imagemUrl);
    }
}
