package br.com.icb.ingressos.ports.in;

/**
 * Caso de uso: autenticar o administrador da igreja (RF-19 / RN-5).
 *
 * <p>Recebe e-mail e senha, valida contra a credencial cadastrada (senha com
 * hash BCrypt) e devolve um token de acesso (JWT) usado nas rotas
 * {@code /api/admin/**} (RF-26). A verificação do token nas requisições
 * seguintes é responsabilidade do adapter de entrada.
 */
public interface AutenticarAdminUseCase {

    /**
     * @throws br.com.icb.ingressos.usecase.CredenciaisInvalidasException
     *         se o e-mail ou a senha não conferem
     */
    TokenEmitido autenticar(Credenciais credenciais);

    record Credenciais(String email, String senha) {
    }

    /**
     * @param token              o JWT assinado
     * @param tipo               esquema de autorização ({@code "Bearer"})
     * @param expiraEmSegundos   validade do token a partir da emissão
     */
    record TokenEmitido(String token, String tipo, long expiraEmSegundos) {
    }
}
