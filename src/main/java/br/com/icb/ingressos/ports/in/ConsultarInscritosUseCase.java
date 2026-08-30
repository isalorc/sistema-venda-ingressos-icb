package br.com.icb.ingressos.ports.in;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso administrativo: listar os inscritos de um evento (RF-25).
 *
 * <p>Inscrito = cliente com pedido {@code PAGO} e o ingresso {@code VENDIDO}
 * correspondente. Exige administrador autenticado (RF-26).
 */
public interface ConsultarInscritosUseCase {

    /**
     * @throws br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException
     *         se o evento não existir
     */
    List<Inscrito> listarPorEvento(Long eventoId);

    record Inscrito(
            String nomeCliente,
            String emailCliente,
            String telefoneCliente,
            Long loteId,
            String nomeLote,
            Long pedidoId,
            String codigoQr,
            LocalDateTime dataCompra) {
    }
}
