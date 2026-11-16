package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.Evento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Espelho de persistência de {@link Evento}. {@code data_hora} = início.
 */
@Entity
@Table(name = "evento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class EventoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Column(name = "imagem_url", length = 2048)
    private String imagemUrl;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    private EventoJpa(Long id, String nome, String descricao, LocalDateTime dataHora,
                      LocalDateTime dataFim, String imagemUrl, LocalDateTime canceladoEm) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.dataHora = dataHora;
        this.dataFim = dataFim;
        this.imagemUrl = imagemUrl;
        this.canceladoEm = canceladoEm;
    }

    static EventoJpa de(Evento evento) {
        return new EventoJpa(evento.getId(), evento.getNome(), evento.getDescricao(),
                evento.getDataHora(), evento.getDataFim(), evento.getImagemUrl(), evento.getCanceladoEm());
    }

    Evento paraDominio() {
        return Evento.reconstituir(id, nome, descricao, dataHora, dataFim, imagemUrl, canceladoEm);
    }
}
