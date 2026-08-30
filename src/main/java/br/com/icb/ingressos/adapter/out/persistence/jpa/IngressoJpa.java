package br.com.icb.ingressos.adapter.out.persistence.jpa;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Espelho de persistência de {@link Ingresso}.
 */
@Entity
@Table(name = "ingresso")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IngressoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "codigo_qr")
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusIngresso status;

    private IngressoJpa(Long id, Long eventoId, Long loteId, Long pedidoId,
                        String codigoQr, StatusIngresso status) {
        this.id = id;
        this.eventoId = eventoId;
        this.loteId = loteId;
        this.pedidoId = pedidoId;
        this.codigoQr = codigoQr;
        this.status = status;
    }

    static IngressoJpa de(Ingresso ingresso) {
        return new IngressoJpa(ingresso.getId(), ingresso.getEventoId(), ingresso.getLoteId(),
                ingresso.getPedidoId(), ingresso.getCodigoQr(), ingresso.getStatus());
    }

    Ingresso paraDominio() {
        return Ingresso.reconstituir(id, eventoId, loteId, pedidoId, codigoQr, status);
    }
}
