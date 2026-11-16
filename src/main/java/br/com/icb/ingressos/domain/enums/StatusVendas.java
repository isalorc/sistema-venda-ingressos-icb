package br.com.icb.ingressos.domain.enums;

/**
 * Resumo do estado de vendas de um evento (ver {@code docs/viradaDeLote.md}),
 * derivado dos status dos seus lotes.
 */
public enum StatusVendas {

    /** Há um lote ativo — o evento está vendendo. */
    A_VENDA,

    /** Nenhum lote ativo agora, mas há lote agendado (acompanha {@code aberturaVendas}). */
    AGENDADA,

    /** Nada a vender nem a abrir: todos os lotes encerrados ou esgotados. */
    ENCERRADA
}
