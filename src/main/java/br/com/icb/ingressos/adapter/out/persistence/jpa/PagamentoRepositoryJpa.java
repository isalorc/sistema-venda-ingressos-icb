package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "postgres")
class PagamentoRepositoryJpa implements PagamentoRepositoryPort {

    private final PagamentoJpaRepository repository;

    PagamentoRepositoryJpa(PagamentoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pagamento salvar(Pagamento pagamento) {
        return repository.save(PagamentoJpa.de(pagamento)).paraDominio();
    }

    @Override
    public Optional<Pagamento> buscarPorId(Long id) {
        return repository.findById(id).map(PagamentoJpa::paraDominio);
    }

    @Override
    public Optional<Pagamento> buscarPorPedido(Long pedidoId) {
        return repository.findByPedidoId(pedidoId).map(PagamentoJpa::paraDominio);
    }

    @Override
    public Optional<Pagamento> buscarPorReferenciaGateway(String referenciaGateway) {
        return referenciaGateway == null ? Optional.empty()
                : repository.findByReferenciaGateway(referenciaGateway).map(PagamentoJpa::paraDominio);
    }
}
