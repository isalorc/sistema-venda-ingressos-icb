package br.com.icb.ingressos.ports.out;

import java.util.List;
import java.util.Optional;

import br.com.icb.ingressos.domain.Evento;

/**
 * Porta de saída para a persistência de {@link Evento}.
 */
public interface EventoRepositoryPort {

    /**
     * Persiste um evento novo (id nulo) ou atualiza um existente.
     *
     * @return o evento persistido, com id atribuído quando novo
     */
    Evento salvar(Evento evento);

    Optional<Evento> buscarPorId(Long id);

    /**
     * Lista todos os eventos cadastrados. O filtro de "eventos disponíveis para
     * compra" (futuros e com lote disponível — RF-01) é regra de negócio e fica
     * no caso de uso.
     */
    List<Evento> listarTodos();
}
