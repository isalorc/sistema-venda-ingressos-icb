package br.com.icb.ingressos.ports.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.enums.StatusIngresso;

/**
 * Porta de saída para a persistência de {@link Ingresso}.
 */
public interface IngressoRepositoryPort {

    /**
     * Persiste um ingresso novo (id nulo) ou atualiza um existente.
     *
     * @return o ingresso persistido, com id atribuído quando novo
     */
    Ingresso salvar(Ingresso ingresso);

    /**
     * Persiste em bloco os ingressos gerados na criação de um lote (RF-23).
     *
     * @return os ingressos persistidos, com ids atribuídos
     */
    List<Ingresso> salvarTodos(List<Ingresso> ingressos);

    Optional<Ingresso> buscarPorId(Long id);

    /**
     * Retorna um ingresso {@code DISPONIVEL} do lote, se houver (RF-06). A
     * segurança contra concorrência vem do bloqueio pessimista no lote
     * ({@link LoteRepositoryPort#buscarPorIdComBloqueio}), que serializa o
     * acesso a este método dentro do fluxo de compra.
     */
    Optional<Ingresso> buscarPrimeiroDisponivelDoLote(Long loteId);

    /**
     * Ingressos de um pedido. Hoje é sempre um (RN-1), mas a assinatura já
     * suporta a evolução para N ingressos por pedido.
     */
    List<Ingresso> listarPorPedido(Long pedidoId);

    /**
     * Ingressos {@code VENDIDO} de um evento, base para a lista de inscritos
     * (RF-25).
     */
    List<Ingresso> listarVendidosDoEvento(Long eventoId);

    /**
     * Quantos ingressos do evento estão em algum dos status informados. Base da
     * checagem "é seguro excluir?" (gestão de eventos).
     */
    long contarPorEventoNosStatus(Long eventoId, Collection<StatusIngresso> status);

    /** Idem, restrito a um lote. */
    long contarPorLoteNosStatus(Long loteId, Collection<StatusIngresso> status);

    /** Remove todos os ingressos de um lote (usado na exclusão em cascata). */
    void excluirPorLote(Long loteId);
}
