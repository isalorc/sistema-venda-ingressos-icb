package br.com.icb.ingressos.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pagamento {

    @EqualsAndHashCode.Include
    private final Long id;
    private final Long pedidoId;
    private final BigDecimal valor;
    private final MetodoPagamento metodo;
    private LocalDateTime dataPagamento;
    private StatusPagamento status;
    private String referenciaGateway;

    private Pagamento(Long id, Long pedidoId, LocalDateTime dataPagamento, BigDecimal valor,
                      MetodoPagamento metodo, StatusPagamento status, String referenciaGateway) {
        this.id = id;
        this.pedidoId = Validacao.exigir(pedidoId, "pedidoId");
        this.valor = Validacao.exigirDinheiro(valor, "valor");
        this.metodo = Validacao.exigir(metodo, "metodo");
        this.dataPagamento = dataPagamento;
        this.status = Validacao.exigir(status, "status");
        this.referenciaGateway = referenciaGateway;
    }

    public static Pagamento pendente(Long pedidoId, BigDecimal valor, MetodoPagamento metodo) {
        return new Pagamento(null, pedidoId, null, valor, metodo, StatusPagamento.PENDENTE, null);
    }

    public static Pagamento reconstituir(Long id, Long pedidoId, LocalDateTime dataPagamento,
                                         BigDecimal valor, MetodoPagamento metodo, StatusPagamento status,
                                         String referenciaGateway) {
        return new Pagamento(Validacao.exigir(id, "id"), pedidoId, dataPagamento, valor, metodo, status,
                referenciaGateway);
    }

    /**
     * Vincula a referência da cobrança gerada no gateway de pagamento. Essa referência
     * é usada depois para correlacionar a notificação do webhook ao pagamento (RF-12),
     * de forma idempotente.
     */
    public void vincularCobranca(String referenciaGateway) {
        exigirPendente("vincular a cobrança de");
        if (this.referenciaGateway != null) {
            throw new TransicaoInvalidaException(
                    "O pagamento %s já possui uma cobrança vinculada.".formatted(id));
        }
        this.referenciaGateway = Validacao.exigirTexto(referenciaGateway, "referenciaGateway");
    }

    public void aprovar(LocalDateTime dataPagamento) {
        exigirPendente("aprovar");
        this.dataPagamento = Validacao.exigir(dataPagamento, "dataPagamento");
        this.status = StatusPagamento.APROVADO;
    }

    public void recusar() {
        exigirPendente("recusar");
        this.status = StatusPagamento.RECUSADO;
    }

    public void cancelar() {
        exigirPendente("cancelar");
        this.status = StatusPagamento.CANCELADO;
    }

    private void exigirPendente(String acao) {
        if (status != StatusPagamento.PENDENTE) {
            throw new TransicaoInvalidaException(
                    "Não é possível %s um pagamento no status %s.".formatted(acao, status));
        }
    }
}
