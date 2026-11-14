package br.com.icb.ingressos.domain.exception;

/**
 * Pedido sem participantes ou acima do limite por pedido (RN-1). Regra violada
 * pela requisição: mapeada para HTTP 422.
 */
public class QuantidadeDeIngressosInvalidaException extends DominioException {

    public QuantidadeDeIngressosInvalidaException(int maximo) {
        super("Informe de 1 a %d participantes por pedido.".formatted(maximo));
    }
}
