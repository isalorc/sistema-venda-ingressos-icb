package br.com.icb.ingressos.ports.out;

import java.util.Optional;

import br.com.icb.ingressos.domain.Usuario;

/**
 * Porta de saída para a persistência de {@link Usuario}.
 *
 * <p>O cliente não tem cadastro próprio (RN-4): na compra ele é resolvido pelo
 * e-mail — criado se novo, reaproveitado se já existir.
 */
public interface UsuarioRepositoryPort {

    /**
     * Persiste um usuário novo (id nulo) ou atualiza um existente.
     *
     * @return o usuário persistido, com id atribuído quando novo
     */
    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    /**
     * Busca um usuário pelo e-mail já normalizado (minúsculo).
     */
    Optional<Usuario> buscarPorEmail(String email);
}
