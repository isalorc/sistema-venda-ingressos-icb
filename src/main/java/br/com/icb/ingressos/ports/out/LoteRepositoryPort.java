package br.com.icb.ingressos.ports.out;

import java.util.List;
import java.util.Optional;

import br.com.icb.ingressos.domain.Lote;

/**
 * Porta de saída para a persistência de {@link Lote}.
 */
public interface LoteRepositoryPort {

    /**
     * Persiste um lote novo (id nulo) ou atualiza um existente.
     *
     * @return o lote persistido, com id atribuído quando novo
     */
    Lote salvar(Lote lote);

    Optional<Lote> buscarPorId(Long id);

    /**
     * Busca um lote adquirindo bloqueio pessimista sobre ele (RNF-07). Usado no
     * fluxo de compra para serializar a reserva de unidades e impedir que a
     * quantidade disponível fique negativa. Deve ser chamado dentro de uma
     * transação.
     */
    Optional<Lote> buscarPorIdComBloqueio(Long id);

    List<Lote> listarPorEvento(Long eventoId);
}
