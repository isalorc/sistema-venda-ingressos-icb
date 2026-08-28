package br.com.icb.ingressos.domain;

import br.com.icb.ingressos.domain.enuns.StatusIngresso;
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
public class Ingresso {
    @EqualsAndHashCode.Include
    private Long id;
    private Long eventoId;
    private Long loteId;
    private Long pedidoId;
    private String codigoQr;
    private StatusIngresso status;
}
