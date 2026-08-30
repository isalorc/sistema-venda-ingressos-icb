package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "postgres")
class LoteRepositoryJpa implements LoteRepositoryPort {

    private final LoteJpaRepository repository;

    LoteRepositoryJpa(LoteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Lote salvar(Lote lote) {
        return repository.save(LoteJpa.de(lote)).paraDominio();
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return repository.findById(id).map(LoteJpa::paraDominio);
    }

    /** RNF-07: {@code SELECT ... FOR UPDATE}. Ver {@link LoteJpaRepository#buscarComBloqueio}. */
    @Override
    public Optional<Lote> buscarPorIdComBloqueio(Long id) {
        return repository.buscarComBloqueio(id).map(LoteJpa::paraDominio);
    }

    @Override
    public List<Lote> listarPorEvento(Long eventoId) {
        return repository.findByEventoId(eventoId).stream().map(LoteJpa::paraDominio).toList();
    }
}
