package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.StatusPedido;
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
 * Espelho de persistência de {@link Pedido}.
 */
@Entity
@Table(name = "pedido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PedidoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "data_pedido", nullable = false)
    private LocalDateTime dataPedido;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status;

    private PedidoJpa(Long id, Long usuarioId, LocalDateTime dataPedido,
                      BigDecimal valorTotal, StatusPedido status) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.dataPedido = dataPedido;
        this.valorTotal = valorTotal;
        this.status = status;
    }

    static PedidoJpa de(Pedido pedido) {
        return new PedidoJpa(pedido.getId(), pedido.getUsuarioId(), pedido.getDataPedido(),
                pedido.getValorTotal(), pedido.getStatus());
    }

    Pedido paraDominio() {
        return Pedido.reconstituir(id, usuarioId, dataPedido, valorTotal, status);
    }
}
