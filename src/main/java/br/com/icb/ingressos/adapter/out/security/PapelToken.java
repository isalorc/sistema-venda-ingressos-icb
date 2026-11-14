package br.com.icb.ingressos.adapter.out.security;

/**
 * Claim {@code papel} dos JWT emitidos pela API. Admin e cliente assinam com a
 * mesma chave, então é o papel que impede um token de cliente de abrir rotas de
 * admin (e vice-versa).
 */
final class PapelToken {

    static final String CLAIM = "papel";
    static final String ADMIN = "ADMIN";
    static final String CLIENTE = "CLIENTE";

    private PapelToken() {
    }
}
