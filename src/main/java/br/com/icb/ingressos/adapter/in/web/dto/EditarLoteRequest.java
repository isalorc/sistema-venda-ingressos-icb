package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.GerenciarLoteUseCase.DadosDoLote;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo do `PUT /api/admin/eventos/{id}/lotes/{loteId}` — substituição dos campos
 * editáveis do lote. {@code quantidadeTotal} só pode aumentar (regra no caso de uso).
 */
public record EditarLoteRequest(

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

    public DadosDoLote toDados() {
        return new DadosDoLote(nome, preco, quantidadeTotal, inicioVendas, fimVendas);
    }
}
