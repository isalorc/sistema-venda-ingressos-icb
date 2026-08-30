package br.com.icb.ingressos.domain;

import java.math.BigDecimal;

import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Lote {

    @EqualsAndHashCode.Include
    private final Long id;
    private final Long eventoId;
    private final String nome;
    private final BigDecimal preco;
    private final int quantidadeTotal;
    private int quantidadeDisponivel;

    private Lote(Long id, Long eventoId, String nome, BigDecimal preco,
                int quantidadeTotal, int quantidadeDisponivel) {
        this.id = id;
        this.eventoId = Validacao.exigir(eventoId, "eventoId");
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.preco = Validacao.exigirDinheiro(preco, "preco");
        this.quantidadeTotal = Validacao.exigirPositivo(quantidadeTotal, "quantidadeTotal");
        if (quantidadeDisponivel < 0 || quantidadeDisponivel > quantidadeTotal) {
            throw new IllegalArgumentException(
                    "quantidadeDisponivel deve estar entre 0 e quantidadeTotal.");
        }
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public static Lote novo(Long eventoId, String nome, BigDecimal preco, int quantidadeTotal) {
        return new Lote(null, eventoId, nome, preco, quantidadeTotal, quantidadeTotal);
    }

    public static Lote reconstituir(Long id, Long eventoId, String nome, BigDecimal preco,
                                    int quantidadeTotal, int quantidadeDisponivel) {
        return new Lote(Validacao.exigir(id, "id"), eventoId, nome, preco,
                quantidadeTotal, quantidadeDisponivel);
    }

    public void reservarUnidade() {
        if (quantidadeDisponivel == 0) {
            throw new IngressoEsgotadoException(id);
        }
        quantidadeDisponivel--;
    }

    public void liberarUnidade() {
        if (quantidadeDisponivel == quantidadeTotal) {
            throw new TransicaoInvalidaException(
                    "O lote " + id + " não possui reservas a liberar.");
        }
        quantidadeDisponivel++;
    }

    public boolean estaEsgotado() {
        return quantidadeDisponivel == 0;
    }

    public int quantidadeReservada() {
        return quantidadeTotal - quantidadeDisponivel;
    }
}
