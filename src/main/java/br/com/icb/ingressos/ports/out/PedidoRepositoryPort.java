package br.com.icb.ingressos.ports.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.icb.ingressos.domain.Pedido;

/**
 * Porta de saída para a persistência de {@link Pedido}.
 */
public interface PedidoRepositoryPort {

    /**
     * Persiste um pedido novo (id nulo) ou atualiza um existente.
     *
     * @return o pedido persistido, com id atribuído quando novo
     */
    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);

    /**
     * Pedidos ainda {@code PENDENTE} cuja data de criação é anterior ao limite
     * informado. Base do job de expiração de reservas (RF-17): o caso de uso
     * passa {@code agora - ttlReserva}.
     */
    List<Pedido> listarPendentesCriadosAntesDe(LocalDateTime limite);
}
