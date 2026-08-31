package br.com.icb.ingressos.domain.exception;

/**
 * Tentativa de comprar de um lote que não é o lote ativo do evento (agendado,
 * encerrado ou ainda na fila) — ver {@code docs/viradaDeLote.md}. Conflito com o
 * estado atual das vendas: mapeada para HTTP 409.
 */
public class LoteIndisponivelException extends DominioException {

    public LoteIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
