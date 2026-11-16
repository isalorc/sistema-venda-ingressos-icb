package br.com.icb.ingressos.domain.exception;

/**
 * Janela de vendas de um lote inconsistente: fim antes do início, fim no
 * passado, ou datas além da {@code dataHora} do evento. Mapeada para HTTP 422
 * pelo {@code GlobalExceptionHandler} (fallback de {@link DominioException}).
 */
public class PeriodoDeVendaInvalidoException extends DominioException {

    public PeriodoDeVendaInvalidoException(String mensagem) {
        super(mensagem);
    }
}
