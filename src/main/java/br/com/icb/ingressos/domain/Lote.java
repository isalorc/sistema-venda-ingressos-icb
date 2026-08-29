package br.com.icb.ingressos.domain;

import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lote {
    @EqualsAndHashCode.Include
    private Long id;
    private Long eventoId;
    private String nome;
    private BigDecimal preco;
    private Integer quantidadeTotal;
    private Integer quantidadeDisponivel;
}
