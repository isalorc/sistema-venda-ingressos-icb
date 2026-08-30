package br.com.icb.ingressos.usecase;

/**
 * Lançada quando o e-mail ou a senha informados no login do administrador não
 * conferem (RF-19). Mapeada para HTTP 401 no {@code GlobalExceptionHandler}.
 *
 * <p>A mensagem é genérica de propósito — não revela se o e-mail existe.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
