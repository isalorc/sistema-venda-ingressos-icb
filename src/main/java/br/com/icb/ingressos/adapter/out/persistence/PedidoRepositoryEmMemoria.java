package br.com.icb.ingressos.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

@Repository
@ConditionalOnProperty(prefix = "app", name = "persistencia", havingValue = "memoria", matchIfMissing = true)
class PedidoRepositoryEmMemoria extends RepositorioEmMemoria<Pedido> implements PedidoRepositoryPort {

    @Override
    public Pedido salvar(Pedido pedido) {
        return persistir(pedido);
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return porId(id);
    }

    @Override
    public List<Pedido> listarPendentesCriadosAntesDe(LocalDateTime limite) {
        return filtrar(pedido -> pedido.getStatus() == StatusPedido.PENDENTE
                && pedido.getDataPedido().isBefore(limite));
    }

    @Override
    protected Long id(Pedido pedido) {
        return pedido.getId();
    }

    @Override
    protected Pedido comId(Pedido pedido, long id) {
        return Pedido.reconstituir(id, pedido.getUsuarioId(), pedido.getDataPedido(),
                pedido.getValorTotal(), pedido.getStatus());
    }
}
