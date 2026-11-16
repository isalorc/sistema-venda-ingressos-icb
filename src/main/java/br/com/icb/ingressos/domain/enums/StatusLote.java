package br.com.icb.ingressos.domain.enums;

/**
 * Status derivado de um lote no fluxo de "virada de lote"
 * (ver {@code docs/viradaDeLote.md}). Nunca persistido — calculado a partir do
 * estoque e da janela de vendas em {@code ClassificacaoDeLotes}.
 *
 * <p>Precedência quando mais de um se aplica:
 * {@code ENCERRADO > AGENDADO > ESGOTADO > A_VENDA / NA_FILA}.
 */
public enum StatusLote {

    /** É o lote ativo do evento — o único comprável agora. */
    A_VENDA,

    /** {@code inicioVendas} no futuro: ainda não abriu. */
    AGENDADO,

    /** {@code fimVendas} no passado: vendas encerradas. */
    ENCERRADO,

    /** Sem estoque, dentro da janela de vendas. */
    ESGOTADO,

    /** Tem estoque e está na janela, mas há um lote anterior ainda ativo. */
    NA_FILA
}
