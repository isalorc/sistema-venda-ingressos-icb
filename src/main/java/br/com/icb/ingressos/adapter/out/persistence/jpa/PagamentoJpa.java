package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
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
 * Espelho de persistência de {@link Pagamento}. A {@code referencia_gateway} é
 * única no banco: sustenta a correlação idempotente do webhook (RF-12).
 */
@Entity
@Table(name = "pagamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PagamentoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Column(name = "referencia_gateway")
    private String referenciaGateway;

    private PagamentoJpa(Long id, Long pedidoId, BigDecimal valor, MetodoPagamento metodo,
                         LocalDateTime dataPagamento, StatusPagamento status, String referenciaGateway) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.valor = valor;
        this.metodo = metodo;
        this.dataPagamento = dataPagamento;
        this.status = status;
        this.referenciaGateway = referenciaGateway;
    }

    static PagamentoJpa de(Pagamento pagamento) {
        return new PagamentoJpa(pagamento.getId(), pagamento.getPedidoId(), pagamento.getValor(),
                pagamento.getMetodo(), pagamento.getDataPagamento(), pagamento.getStatus(),
                pagamento.getReferenciaGateway());
    }

    Pagamento paraDominio() {
        return Pagamento.reconstituir(id, pedidoId, dataPagamento, valor, metodo, status, referenciaGateway);
    }
}
