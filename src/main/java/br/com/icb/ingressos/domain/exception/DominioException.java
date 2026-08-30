package br.com.icb.ingressos.domain.exception;

public abstract class DominioException extends RuntimeException {

    protected DominioException(String mensagem) {
        super(mensagem);
    }
}
