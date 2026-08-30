package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "postgres")
class PedidoRepositoryJpa implements PedidoRepositoryPort {

    private final PedidoJpaRepository repository;

    PedidoRepositoryJpa(PedidoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        return repository.save(PedidoJpa.de(pedido)).paraDominio();
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return repository.findById(id).map(PedidoJpa::paraDominio);
    }

    @Override
    public List<Pedido> listarPendentesCriadosAntesDe(LocalDateTime limite) {
        return repository.findByStatusAndDataPedidoBefore(StatusPedido.PENDENTE, limite)
                .stream().map(PedidoJpa::paraDominio).toList();
    }
}
