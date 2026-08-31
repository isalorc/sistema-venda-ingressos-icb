package br.com.icb.ingressos.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.domain.enums.StatusLote;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase.DetalheEvento;

/**
 * Detalhes de um evento e seus lotes (RF-02). Cada lote traz o {@code status}
 * derivado da virada de lote ({@code docs/viradaDeLote.md}) e a janela de vendas.
 */
public record EventoDetalheResponse(
        Long id,
        String nome,
        String descricao,
        LocalDateTime dataHora,
        LocalDateTime dataFim,
        String imagemUrl,
        boolean cancelado,
        List<LoteResponse> lotes) {

    public record LoteResponse(
            Long id,
            String nome,
            BigDecimal preco,
            int quantidadeDisponivel,
            StatusLote status,
            LocalDateTime inicioVendas,
            LocalDateTime fimVendas) {
    }

    public static EventoDetalheResponse de(DetalheEvento evento) {
        var lotes = evento.lotes().stream()
                .map(lote -> new LoteResponse(
                        lote.loteId(), lote.nome(), lote.preco(), lote.quantidadeDisponivel(),
                        lote.status(), lote.inicioVendas(), lote.fimVendas()))
                .toList();
        return new EventoDetalheResponse(
                evento.eventoId(), evento.nome(), evento.descricao(), evento.dataHora(),
                evento.dataFim(), evento.imagemUrl(), evento.cancelado(), lotes);
    }
}
