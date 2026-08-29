package br.com.icb.ingressos.domain;

import java.time.LocalDateTime;
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
public class Evento {
    @EqualsAndHashCode.Include
    private Long id;
    private String nome;
    private String descricao;
    private LocalDateTime dataHora;
}
