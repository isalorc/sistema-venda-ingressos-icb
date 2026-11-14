package br.com.icb.ingressos.domain;

import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ingresso {

    @EqualsAndHashCode.Include
    private final Long id;
    private final Long eventoId;
    private final Long loteId;
    private Long pedidoId;
    /** Quem vai usar o ingresso (RN-1: nome por ingresso). Nulo enquanto disponível. */
    private String nomeParticipante;
    private String codigoQr;
    private StatusIngresso status;

    private Ingresso(Long id, Long eventoId, Long loteId, Long pedidoId, String nomeParticipante,
                     String codigoQr, StatusIngresso status) {
        this.id = id;
        this.eventoId = Validacao.exigir(eventoId, "eventoId");
        this.loteId = Validacao.exigir(loteId, "loteId");
        this.pedidoId = pedidoId;
        this.nomeParticipante = nomeParticipante;
        this.codigoQr = codigoQr;
        this.status = Validacao.exigir(status, "status");
    }

    public static Ingresso disponivel(Long eventoId, Long loteId) {
        return new Ingresso(null, eventoId, loteId, null, null, null, StatusIngresso.DISPONIVEL);
    }

    public static Ingresso reconstituir(Long id, Long eventoId, Long loteId, Long pedidoId,
                                        String codigoQr, StatusIngresso status) {
        return reconstituir(id, eventoId, loteId, pedidoId, null, codigoQr, status);
    }

    public static Ingresso reconstituir(Long id, Long eventoId, Long loteId, Long pedidoId,
                                        String nomeParticipante, String codigoQr, StatusIngresso status) {
        return new Ingresso(Validacao.exigir(id, "id"), eventoId, loteId, pedidoId,
                nomeParticipante, codigoQr, status);
    }

    public void reservar(Long pedidoId) {
        exigirStatus(StatusIngresso.DISPONIVEL, "reservar");
        this.pedidoId = Validacao.exigir(pedidoId, "pedidoId");
        this.status = StatusIngresso.RESERVADO;
    }

    /** Reserva o ingresso para o pedido, em nome de um participante. */
    public void reservar(Long pedidoId, String nomeParticipante) {
        var nome = Validacao.exigirTexto(nomeParticipante, "nomeParticipante").strip();
        reservar(pedidoId);
        this.nomeParticipante = nome;
    }

    public void confirmarVenda(String codigoQr) {
        exigirStatus(StatusIngresso.RESERVADO, "confirmar a venda de");
        this.codigoQr = Validacao.exigirTexto(codigoQr, "codigoQr");
        this.status = StatusIngresso.VENDIDO;
    }

    public void liberarReserva() {
        exigirStatus(StatusIngresso.RESERVADO, "liberar a reserva de");
        this.pedidoId = null;
        this.nomeParticipante = null;
        this.status = StatusIngresso.DISPONIVEL;
    }

    public void utilizar() {
        exigirStatus(StatusIngresso.VENDIDO, "utilizar");
        this.status = StatusIngresso.UTILIZADO;
    }

    private void exigirStatus(StatusIngresso esperado, String acao) {
        if (this.status != esperado) {
            throw new TransicaoInvalidaException(
                    "Não é possível %s um ingresso no status %s.".formatted(acao, status));
        }
    }
}
