package br.com.icb.ingressos.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pedido {

    @EqualsAndHashCode.Include
    private final Long id;
    private final Long usuarioId;
    private final LocalDateTime dataPedido;
    private final BigDecimal valorTotal;
    private StatusPedido status;

    private Pedido(Long id, Long usuarioId, LocalDateTime dataPedido,
                   BigDecimal valorTotal, StatusPedido status) {
        this.id = id;
        this.usuarioId = Validacao.exigir(usuarioId, "usuarioId");
        this.dataPedido = Validacao.exigir(dataPedido, "dataPedido");
        this.valorTotal = Validacao.exigirDinheiro(valorTotal, "valorTotal");
        this.status = Validacao.exigir(status, "status");
    }

    public static Pedido novo(Long usuarioId, LocalDateTime dataPedido, BigDecimal valorTotal) {
        return new Pedido(null, usuarioId, dataPedido, valorTotal, StatusPedido.PENDENTE);
    }

    public static Pedido reconstituir(Long id, Long usuarioId, LocalDateTime dataPedido,
                                      BigDecimal valorTotal, StatusPedido status) {
        return new Pedido(Validacao.exigir(id, "id"), usuarioId, dataPedido, valorTotal, status);
    }

    public void marcarPago() {
        exigirPendente("marcar como pago");
        this.status = StatusPedido.PAGO;
    }

    public void cancelar() {
        exigirPendente("cancelar");
        this.status = StatusPedido.CANCELADO;
    }

    public void expirar() {
        exigirPendente("expirar");
        this.status = StatusPedido.EXPIRADO;
    }

    public boolean reservaExpirada(LocalDateTime referencia, Duration ttlReserva) {
        Validacao.exigir(referencia, "referencia");
        Validacao.exigir(ttlReserva, "ttlReserva");
        return status == StatusPedido.PENDENTE
                && !dataPedido.plus(ttlReserva).isAfter(referencia);
    }

    private void exigirPendente(String acao) {
        if (status != StatusPedido.PENDENTE) {
            throw new TransicaoInvalidaException(
                    "Não é possível %s um pedido no status %s.".formatted(acao, status));
        }
    }
}
