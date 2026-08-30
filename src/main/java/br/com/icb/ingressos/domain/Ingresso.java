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
    private String codigoQr;
    private StatusIngresso status;

    private Ingresso(Long id, Long eventoId, Long loteId, Long pedidoId,
                     String codigoQr, StatusIngresso status) {
        this.id = id;
        this.eventoId = Validacao.exigir(eventoId, "eventoId");
        this.loteId = Validacao.exigir(loteId, "loteId");
        this.pedidoId = pedidoId;
        this.codigoQr = codigoQr;
        this.status = Validacao.exigir(status, "status");
    }

    public static Ingresso disponivel(Long eventoId, Long loteId) {
        return new Ingresso(null, eventoId, loteId, null, null, StatusIngresso.DISPONIVEL);
    }

    public static Ingresso reconstituir(Long id, Long eventoId, Long loteId, Long pedidoId,
                                        String codigoQr, StatusIngresso status) {
        return new Ingresso(Validacao.exigir(id, "id"), eventoId, loteId, pedidoId, codigoQr, status);
    }

    public void reservar(Long pedidoId) {
        exigirStatus(StatusIngresso.DISPONIVEL, "reservar");
        this.pedidoId = Validacao.exigir(pedidoId, "pedidoId");
        this.status = StatusIngresso.RESERVADO;
    }

    public void confirmarVenda(String codigoQr) {
        exigirStatus(StatusIngresso.RESERVADO, "confirmar a venda de");
        this.codigoQr = Validacao.exigirTexto(codigoQr, "codigoQr");
        this.status = StatusIngresso.VENDIDO;
    }

    public void liberarReserva() {
        exigirStatus(StatusIngresso.RESERVADO, "liberar a reserva de");
        this.pedidoId = null;
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
