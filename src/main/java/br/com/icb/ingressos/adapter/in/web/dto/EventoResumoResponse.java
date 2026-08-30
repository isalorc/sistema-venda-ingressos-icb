package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase.EventoDisponivel;

/**
 * Resumo de um evento disponível para a tela de listagem (RF-01).
 */
public record EventoResumoResponse(
        Long id,
        String nome,
        String descricao,
        LocalDateTime dataHora,
        BigDecimal menorPreco,
        int ingressosDisponiveis) {

    public static EventoResumoResponse de(EventoDisponivel evento) {
        return new EventoResumoResponse(
                evento.eventoId(),
                evento.nome(),
                evento.descricao(),
                evento.dataHora(),
                evento.menorPreco(),
                evento.ingressosDisponiveis());
    }
}
