package br.com.icb.ingressos.adapter.in.web.dto;

import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase.TokenEmitido;

/**
 * Corpo de resposta do login: o token de acesso e como usá-lo.
 *
 * <p>Nas rotas {@code /api/admin/**} envie {@code Authorization: Bearer <token>}.
 */
public record TokenResponse(String token, String tipo, long expiraEmSegundos) {

    public static TokenResponse de(TokenEmitido emitido) {
        return new TokenResponse(emitido.token(), emitido.tipo(), emitido.expiraEmSegundos());
    }
}
