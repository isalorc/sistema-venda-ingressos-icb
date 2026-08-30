package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase.DetalheEvento;

/**
 * Detalhes de um evento e seus lotes (RF-02).
 */
public record EventoDetalheResponse(
        Long id,
        String nome,
        String descricao,
        LocalDateTime dataHora,
        List<LoteResponse> lotes) {

    public record LoteResponse(
            Long id,
            String nome,
            BigDecimal preco,
            int quantidadeDisponivel) {
    }

    public static EventoDetalheResponse de(DetalheEvento evento) {
        var lotes = evento.lotes().stream()
                .map(lote -> new LoteResponse(
                        lote.loteId(), lote.nome(), lote.preco(), lote.quantidadeDisponivel()))
                .toList();
        return new EventoDetalheResponse(
                evento.eventoId(), evento.nome(), evento.descricao(), evento.dataHora(), lotes);
    }
}
