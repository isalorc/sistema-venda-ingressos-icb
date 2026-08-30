package br.com.icb.ingressos.ports.out;

import java.time.LocalDateTime;

/**
 * Porta de saída para notificar o cliente (RF-15).
 *
 * <p>No MVP a implementação é um <em>stub</em> até a definição do template de
 * e-mail do ingresso (pendência registrada em {@code docs/regrasDeNegocio.md}).
 */
public interface NotificacaoPort {

    /**
     * Envia ao cliente o ingresso já vendido, com o código QR definitivo.
     */
    void enviarIngresso(IngressoEmitido ingresso);

    record IngressoEmitido(
            String nomeCliente,
            String emailCliente,
            String nomeEvento,
            LocalDateTime dataHoraEvento,
            String codigoQr) {
    }
}
