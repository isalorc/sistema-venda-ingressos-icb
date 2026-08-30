package br.com.icb.ingressos.ports.out;

import java.util.Optional;

import br.com.icb.ingressos.domain.Pagamento;

/**
 * Porta de saída para a persistência de {@link Pagamento}.
 */
public interface PagamentoRepositoryPort {

    /**
     * Persiste um pagamento novo (id nulo) ou atualiza um existente.
     *
     * @return o pagamento persistido, com id atribuído quando novo
     */
    Pagamento salvar(Pagamento pagamento);

    Optional<Pagamento> buscarPorId(Long id);

    Optional<Pagamento> buscarPorPedido(Long pedidoId);

    /**
     * Busca o pagamento pela referência da cobrança no gateway. Usado pelo
     * processamento do webhook para correlacionar a notificação recebida ao
     * pagamento de forma idempotente (RF-12).
     */
    Optional<Pagamento> buscarPorReferenciaGateway(String referenciaGateway);
}
