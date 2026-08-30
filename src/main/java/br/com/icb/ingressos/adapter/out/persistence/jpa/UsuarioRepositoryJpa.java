package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "postgres")
class UsuarioRepositoryJpa implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repository;

    UsuarioRepositoryJpa(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        return repository.save(UsuarioJpa.de(usuario)).paraDominio();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id).map(UsuarioJpa::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return email == null ? Optional.empty()
                : repository.findByEmail(email).map(UsuarioJpa::paraDominio);
    }
}
