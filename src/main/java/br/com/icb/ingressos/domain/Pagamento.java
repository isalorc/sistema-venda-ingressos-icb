package br.com.icb.ingressos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
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
public class Pagamento {
    @EqualsAndHashCode.Include
    private Long id;
    private Long pedidoId;
    private LocalDateTime dataPagamento;
    private BigDecimal valor;
    private MetodoPagamento metodo;
    private StatusPagamento status;
}
