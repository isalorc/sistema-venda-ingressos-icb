package br.com.icb.ingressos.usecase;

import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException;

/**
 * Regras da janela de vendas de um lote (virada de lote — {@code docs/viradaDeLote.md}),
 * compartilhadas entre criação de lote, edição de lote e edição da data do
 * evento. Violação → 422.
 */
final class ValidadorDeJanelaDeVendas {

    private ValidadorDeJanelaDeVendas() {
    }

    /** Cadastro/edição de lote: consistência + não pode estar no passado + cabe no evento. */
    static void validarNoLote(LocalDateTime inicioVendas, LocalDateTime fimVendas,
                              LocalDateTime terminoDoEvento, LocalDateTime agora) {
        if (inicioVendas != null && fimVendas != null && fimVendas.isBefore(inicioVendas)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O fim das vendas não pode ser anterior ao início.");
        }
        if (fimVendas != null && fimVendas.isBefore(agora)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O fim das vendas não pode estar no passado.");
        }
        validarDentroDoEvento(inicioVendas, fimVendas, terminoDoEvento);
    }

    /**
     * Só a checagem "cabe no evento" — usada ao mudar a data do evento, sem
     * reprovar lotes cuja janela já é histórica.
     */
    static void validarDentroDoEvento(LocalDateTime inicioVendas, LocalDateTime fimVendas,
                                      LocalDateTime terminoDoEvento) {
        if (inicioVendas != null && inicioVendas.isAfter(terminoDoEvento)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O início das vendas de um lote não pode ser depois do fim do evento.");
        }
        if (fimVendas != null && fimVendas.isAfter(terminoDoEvento)) {
            throw new PeriodoDeVendaInvalidoException(
                    "O fim das vendas de um lote não pode ser depois do fim do evento.");
        }
    }
}
