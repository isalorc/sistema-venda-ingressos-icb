package br.com.icb.ingressos.usecase;

/** Cadastro com e-mail que já tem conta. Mapeada para HTTP 409. */
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Já existe uma conta com este e-mail. Entre com sua senha.");
    }
}
