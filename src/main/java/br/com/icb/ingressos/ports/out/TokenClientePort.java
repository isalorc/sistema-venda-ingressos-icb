package br.com.icb.ingressos.ports.out;

import java.util.Optional;

/**
 * Emissão e validação do token de acesso do cliente (área do cliente). O token
 * identifica o cliente pelo id do {@code Usuario}.
 */
public interface TokenClientePort {

    String gerar(Long usuarioId);

    /** Id do cliente se o token for válido, de cliente e não expirado. */
    Optional<Long> validar(String token);
}
