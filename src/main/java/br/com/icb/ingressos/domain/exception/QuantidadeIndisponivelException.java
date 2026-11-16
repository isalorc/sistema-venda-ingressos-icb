package br.com.icb.ingressos.domain.exception;

/**
 * O lote ainda tem ingressos, mas menos do que o pedido pede. Conflito com o
 * estoque atual: mapeada para HTTP 409.
 */
public class QuantidadeIndisponivelException extends DominioException {

    public QuantidadeIndisponivelException(int restantes) {
        super(restantes == 1
                ? "Resta apenas 1 ingresso neste lote."
                : "Restam apenas %d ingressos neste lote.".formatted(restantes));
    }
}
