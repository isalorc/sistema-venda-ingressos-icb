package br.com.icb.ingressos.domain.exception;

public class IngressoEsgotadoException extends DominioException {

    public IngressoEsgotadoException(Long loteId) {
        super("O lote " + loteId + " não possui ingressos disponíveis.");
    }
}
