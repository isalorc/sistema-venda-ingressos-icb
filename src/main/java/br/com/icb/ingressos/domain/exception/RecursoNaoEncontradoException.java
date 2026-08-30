package br.com.icb.ingressos.domain.exception;

public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super(recurso + " não encontrado(a): " + identificador);
    }
}
