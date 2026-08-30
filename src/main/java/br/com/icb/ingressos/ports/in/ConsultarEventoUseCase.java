package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: consultar os detalhes de um evento e seus lotes (RF-02).
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
                int quantidadeDisponivel) {
        }
    }
}
