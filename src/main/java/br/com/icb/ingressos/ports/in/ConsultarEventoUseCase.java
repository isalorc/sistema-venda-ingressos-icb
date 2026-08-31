package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.domain.enums.StatusLote;

/**
 * Caso de uso: consultar os detalhes de um evento e seus lotes (RF-02).
 *
 * <p>Devolve <strong>todos</strong> os lotes, cada um com o {@link StatusLote}
 * derivado (virada de lote — {@code docs/viradaDeLote.md}); cabe ao consumidor
 * decidir o que fica selecionável (só {@code A_VENDA}).
 */
public interface ConsultarEventoUseCase {

    /**
     * @throws br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException
     *         se o evento não existir
     */
    DetalheEvento consultar(Long eventoId);

    record DetalheEvento(
            Long eventoId,
            String nome,
            String descricao,
            LocalDateTime dataHora,
            List<LoteDisponivel> lotes) {

        public record LoteDisponivel(
                Long loteId,
                String nome,
                BigDecimal preco,
                int quantidadeDisponivel,
                StatusLote status,
                LocalDateTime inicioVendas,
                LocalDateTime fimVendas) {
        }
    }
}
