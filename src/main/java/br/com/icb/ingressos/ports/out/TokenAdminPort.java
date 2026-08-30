package br.com.icb.ingressos.ports.out;

import java.util.Optional;

/**
 * Porta de saída para emissão e verificação do token de acesso do administrador
 * (RN-5). Esconde a tecnologia do token (hoje um JWT assinado com HMAC-SHA256);
 * trocá-la por outra implementação não afeta o caso de uso nem o filtro de
 * segurança.
 */
public interface TokenAdminPort {

    /**
     * Emite um token para o administrador identificado pelo e-mail.
     */
    String gerar(String emailAdmin);

    /**
     * Verifica a assinatura e a validade do token.
     *
     * @return o e-mail do administrador se o token é autêntico e não expirou;
     *         {@link Optional#empty()} caso contrário
     */
    Optional<String> validar(String token);
}
