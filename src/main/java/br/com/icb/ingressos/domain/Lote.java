package br.com.icb.ingressos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    /** Abertura das vendas do lote (RN — virada de lote). Nulo = já aberto. */
    private final LocalDateTime inicioVendas;
    /** Fim das vendas do lote. Nulo = sem prazo. */
    private final LocalDateTime fimVendas;

    private Lote(Long id, Long eventoId, String nome, BigDecimal preco,
                int quantidadeTotal, int quantidadeDisponivel,
                LocalDateTime inicioVendas, LocalDateTime fimVendas) {
        this.id = id;
        this.eventoId = Validacao.exigir(eventoId, "eventoId");
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.preco = Validacao.exigirDinheiro(preco, "preco");
        this.quantidadeTotal = Validacao.exigirPositivo(quantidadeTotal, "quantidadeTotal");
        if (quantidadeDisponivel < 0 || quantidadeDisponivel > quantidadeTotal) {
            throw new IllegalArgumentException(
                    "quantidadeDisponivel deve estar entre 0 e quantidadeTotal.");
        }
        if (inicioVendas != null && fimVendas != null && fimVendas.isBefore(inicioVendas)) {
            throw new IllegalArgumentException(
                    "fimVendas não pode ser anterior a inicioVendas.");
        }
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.inicioVendas = inicioVendas;
        this.fimVendas = fimVendas;
    }

    public static Lote novo(Long eventoId, String nome, BigDecimal preco, int quantidadeTotal) {
        return novo(eventoId, nome, preco, quantidadeTotal, null, null);
    }

    public static Lote novo(Long eventoId, String nome, BigDecimal preco, int quantidadeTotal,
                            LocalDateTime inicioVendas, LocalDateTime fimVendas) {
        return new Lote(null, eventoId, nome, preco, quantidadeTotal, quantidadeTotal,
                inicioVendas, fimVendas);
    }

    public static Lote reconstituir(Long id, Long eventoId, String nome, BigDecimal preco,
                                    int quantidadeTotal, int quantidadeDisponivel) {
        return reconstituir(id, eventoId, nome, preco, quantidadeTotal, quantidadeDisponivel, null, null);
    }

    public static Lote reconstituir(Long id, Long eventoId, String nome, BigDecimal preco,
                                    int quantidadeTotal, int quantidadeDisponivel,
                                    LocalDateTime inicioVendas, LocalDateTime fimVendas) {
        return new Lote(Validacao.exigir(id, "id"), eventoId, nome, preco,
                quantidadeTotal, quantidadeDisponivel, inicioVendas, fimVendas);
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

    /** As vendas já abriram na referência informada (ou não têm data de abertura). */
    public boolean vendasIniciadas(LocalDateTime referencia) {
        Validacao.exigir(referencia, "referencia");
        return inicioVendas == null || !inicioVendas.isAfter(referencia);
    }

    /** As vendas ainda não encerraram na referência informada (ou não têm prazo). */
    public boolean vendasNaoEncerradas(LocalDateTime referencia) {
        Validacao.exigir(referencia, "referencia");
        return fimVendas == null || !referencia.isAfter(fimVendas);
    }

    /** Dentro da janela de vendas configurada. */
    public boolean dentroDaJanelaDeVendas(LocalDateTime referencia) {
        return vendasIniciadas(referencia) && vendasNaoEncerradas(referencia);
    }

    /** Elegível a ser o lote ativo do evento: tem estoque e está na janela. */
    public boolean disponivelParaVenda(LocalDateTime referencia) {
        return !estaEsgotado() && dentroDaJanelaDeVendas(referencia);
    }
}
