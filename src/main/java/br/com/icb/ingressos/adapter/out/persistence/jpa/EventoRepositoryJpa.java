package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;

/**
 * Adapter de persistência de {@link Evento} sobre Spring Data JPA / PostgreSQL
 * (Épico 9). Ativo com {@code app.persistencia=postgres}; caso contrário vale o
 * adapter em memória.
 */
@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "postgres")
class EventoRepositoryJpa implements EventoRepositoryPort {

    private final EventoJpaRepository repository;

    EventoRepositoryJpa(EventoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Evento salvar(Evento evento) {
        return repository.save(EventoJpa.de(evento)).paraDominio();
    }

    @Override
    public Optional<Evento> buscarPorId(Long id) {
        return repository.findById(id).map(EventoJpa::paraDominio);
    }

    @Override
    public List<Evento> listarTodos() {
        return repository.findAll().stream().map(EventoJpa::paraDominio).toList();
    }

    @Override
    public void excluir(Long id) {
        repository.deleteById(id);
    }
}
