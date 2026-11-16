package br.com.icb.ingressos.ports.in;

/**
 * Caso de uso: conta do cliente (área do cliente). Cadastro e login devolvem um
 * token de acesso; comprar ingresso e ver pedidos exigem esse token.
 */
public interface ContaClienteUseCase {

    /**
     * Cria a conta. Se o e-mail já comprou antes sem conta (fluxo antigo), ativa a
     * conta desse cadastro, que passa a ver aqueles pedidos.
     */
    TokenCliente cadastrar(Cadastro cadastro);

    TokenCliente autenticar(Credenciais credenciais);

    Perfil perfil(Long usuarioId);

    record Cadastro(String nome, String email, String telefone, String senha) {
    }

    record Credenciais(String email, String senha) {
    }

    record TokenCliente(String token, String tipo, long expiraEmSegundos) {
    }

    record Perfil(Long id, String nome, String email, String telefone) {
    }
}
