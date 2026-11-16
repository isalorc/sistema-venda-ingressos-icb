package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.icb.ingressos.domain.enums.StatusVendas;
import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase.EventoDisponivel;

/**
 * Resumo de um evento disponível para a tela de listagem (RF-01).
 * {@code menorPreco} e {@code ingressosDisponiveis} refletem o lote à venda — ou,
 * quando o evento está {@code AGENDADA}, o próximo lote a abrir ({@code aberturaVendas}).
 */
public record EventoResumoResponse(
        Long id,
        String nome,
        String descricao,
        LocalDateTime dataHora,
        LocalDateTime dataFim,
        String imagemUrl,
        BigDecimal menorPreco,
        int ingressosDisponiveis,
        StatusVendas statusVendas,
        LocalDateTime aberturaVendas) {

    public static EventoResumoResponse de(EventoDisponivel evento) {
        return new EventoResumoResponse(
                evento.eventoId(),
                evento.nome(),
                evento.descricao(),
                evento.dataHora(),
                evento.dataFim(),
                evento.imagemUrl(),
                evento.menorPreco(),
                evento.ingressosDisponiveis(),
                evento.statusVendas(),
                evento.aberturaVendas());
    }
}
