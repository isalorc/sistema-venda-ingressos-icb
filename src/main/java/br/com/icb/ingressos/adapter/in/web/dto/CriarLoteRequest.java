package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.CriarLoteUseCase.CriarLoteCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo da requisição de criação de lote (RF-22). O id do evento vem na URL.
 *
 * <p>{@code inicioVendas} e {@code fimVendas} são opcionais e governam a virada
 * de lote ({@code docs/viradaDeLote.md}). Demais regras da janela (fim &ge;
 * início, datas dentro do evento) são validadas no caso de uso &rarr; 422.
 */
public record CriarLoteRequest(

        @NotBlank(message = "O nome do lote é obrigatório.")
        String nome,

        @NotNull(message = "O preço é obrigatório.")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
        BigDecimal preco,

        @NotNull(message = "A quantidade total é obrigatória.")
        @Positive(message = "A quantidade total deve ser maior que zero.")
        Integer quantidadeTotal,

        LocalDateTime inicioVendas,

        @Future(message = "O fim das vendas deve estar no futuro.")
        LocalDateTime fimVendas) {

    public CriarLoteCommand toCommand(Long eventoId) {
        return new CriarLoteCommand(eventoId, nome, preco, quantidadeTotal, inicioVendas, fimVendas);
    }
}
