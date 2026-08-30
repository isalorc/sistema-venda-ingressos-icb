package br.com.icb.ingressos.ports.in;

/**
 * Caso de uso: expirar as reservas não pagas (RF-17, RF-18 / RN-2).
 *
 * <p>Disparado periodicamente pelo scheduler (adapter de entrada). Para cada
 * pedido {@code PENDENTE} criado há mais tempo que o TTL de reserva
 * ({@code app.reserva.ttl}, 15 min por padrão): {@code Pedido} → EXPIRADO,
 * ingresso volta a {@code DISPONIVEL}, o estoque do lote é reposto e o
 * pagamento pendente é cancelado.
 */
public interface ExpirarReservasUseCase {

    /**
     * @return quantos pedidos foram expirados nesta execução
     */
    int expirarReservasVencidas();
}
