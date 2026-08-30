package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "postgres")
class IngressoRepositoryJpa implements IngressoRepositoryPort {

    private final IngressoJpaRepository repository;

    IngressoRepositoryJpa(IngressoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Ingresso salvar(Ingresso ingresso) {
        return repository.save(IngressoJpa.de(ingresso)).paraDominio();
    }

    @Override
    public List<Ingresso> salvarTodos(List<Ingresso> ingressos) {
        return repository.saveAll(ingressos.stream().map(IngressoJpa::de).toList())
                .stream().map(IngressoJpa::paraDominio).toList();
    }

    @Override
    public Optional<Ingresso> buscarPorId(Long id) {
        return repository.findById(id).map(IngressoJpa::paraDominio);
    }

    @Override
    public Optional<Ingresso> buscarPrimeiroDisponivelDoLote(Long loteId) {
        return repository.findFirstByLoteIdAndStatus(loteId, StatusIngresso.DISPONIVEL)
                .map(IngressoJpa::paraDominio);
    }

    @Override
    public List<Ingresso> listarPorPedido(Long pedidoId) {
        return pedidoId == null ? List.of()
                : repository.findByPedidoId(pedidoId).stream().map(IngressoJpa::paraDominio).toList();
    }

    @Override
    public List<Ingresso> listarVendidosDoEvento(Long eventoId) {
        return repository.findByEventoIdAndStatus(eventoId, StatusIngresso.VENDIDO)
                .stream().map(IngressoJpa::paraDominio).toList();
    }
}
