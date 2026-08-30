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

    private Pagamento(Long id, Long pedidoId, LocalDateTime dataPagamento, BigDecimal valor,
                      MetodoPagamento metodo, StatusPagamento status) {
        this.id = id;
        this.pedidoId = Validacao.exigir(pedidoId, "pedidoId");
        this.valor = Validacao.exigirDinheiro(valor, "valor");
        this.metodo = Validacao.exigir(metodo, "metodo");
        this.dataPagamento = dataPagamento;
        this.status = Validacao.exigir(status, "status");
    }

    public static Pagamento pendente(Long pedidoId, BigDecimal valor, MetodoPagamento metodo) {
        return new Pagamento(null, pedidoId, null, valor, metodo, StatusPagamento.PENDENTE);
    }

    public static Pagamento reconstituir(Long id, Long pedidoId, LocalDateTime dataPagamento,
                                         BigDecimal valor, MetodoPagamento metodo, StatusPagamento status) {
        return new Pagamento(Validacao.exigir(id, "id"), pedidoId, dataPagamento, valor, metodo, status);
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
