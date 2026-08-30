package br.com.icb.ingressos.adapter.out.persistence;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "memoria", matchIfMissing = true)
class UsuarioRepositoryEmMemoria extends RepositorioEmMemoria<Usuario> implements UsuarioRepositoryPort {

    @Override
    public Usuario salvar(Usuario usuario) {
        return persistir(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return porId(id);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return email == null ? Optional.empty() : primeiro(usuario -> usuario.getEmail().equals(email));
    }

    @Override
    protected Long id(Usuario usuario) {
        return usuario.getId();
    }

    @Override
    protected Usuario comId(Usuario usuario, long id) {
        return Usuario.reconstituir(id, usuario.getNome(), usuario.getEmail(),
                usuario.getTelefone(), usuario.getDataCriacao());
    }
}
