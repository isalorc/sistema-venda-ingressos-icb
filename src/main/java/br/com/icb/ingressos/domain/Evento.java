package br.com.icb.ingressos.domain;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Evento {

    @EqualsAndHashCode.Include
    private final Long id;
    private final String nome;
    private final String descricao;
    private final LocalDateTime dataHora;

    private Evento(Long id, String nome, String descricao, LocalDateTime dataHora) {
        this.id = id;
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.descricao = descricao == null ? null : descricao.strip();
        this.dataHora = Validacao.exigir(dataHora, "dataHora");
    }

    public static Evento novo(String nome, String descricao, LocalDateTime dataHora) {
        return new Evento(null, nome, descricao, dataHora);
    }

    public static Evento reconstituir(Long id, String nome, String descricao, LocalDateTime dataHora) {
        return new Evento(Validacao.exigir(id, "id"), nome, descricao, dataHora);
    }

    public boolean jaOcorreu(LocalDateTime referencia) {
        return dataHora.isBefore(Validacao.exigir(referencia, "referencia"));
    }
}
