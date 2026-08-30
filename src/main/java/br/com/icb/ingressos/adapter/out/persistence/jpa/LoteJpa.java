package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.math.BigDecimal;

import br.com.icb.ingressos.domain.Lote;
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
 * Espelho de persistência de {@link Lote}. A invariante de estoque
 * ({@code quantidade_disponivel} entre 0 e {@code quantidade_total}) é do
 * domínio; o banco a reforça com um CHECK (defesa em profundidade).
 */
@Entity
@Table(name = "lote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class LoteJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_total", nullable = false)
    private int quantidadeTotal;

    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;

    private LoteJpa(Long id, Long eventoId, String nome, BigDecimal preco,
                    int quantidadeTotal, int quantidadeDisponivel) {
        this.id = id;
        this.eventoId = eventoId;
        this.nome = nome;
        this.preco = preco;
        this.quantidadeTotal = quantidadeTotal;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    static LoteJpa de(Lote lote) {
        return new LoteJpa(lote.getId(), lote.getEventoId(), lote.getNome(), lote.getPreco(),
                lote.getQuantidadeTotal(), lote.getQuantidadeDisponivel());
    }

    Lote paraDominio() {
        return Lote.reconstituir(id, eventoId, nome, preco, quantidadeTotal, quantidadeDisponivel);
    }
}
