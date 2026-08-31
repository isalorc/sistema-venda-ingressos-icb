package br.com.icb.ingressos.adapter.in.web.dto;

import java.time.LocalDateTime;

import br.com.icb.ingressos.ports.in.ListarEventosAdminUseCase.EventoAdmin;

/**
 * Item da listagem administrativa de eventos (RF-21). Traz o resumo dos lotes
 * para o painel distinguir um evento sem ingressos ({@code quantidadeLotes: 0})
 * de um já configurado.
 */
public record EventoAdminResponse(
        Long id,
        String nome,
        String descricao,
        LocalDateTime dataHora,
        boolean jaOcorreu,
        int quantidadeLotes,
        int ingressosTotais,
        int ingressosDisponiveis) {

    public static EventoAdminResponse de(EventoAdmin evento) {
        return new EventoAdminResponse(
                evento.id(),
                evento.nome(),
                evento.descricao(),
                evento.dataHora(),
                evento.jaOcorreu(),
                evento.quantidadeLotes(),
                evento.ingressosTotais(),
                evento.ingressosDisponiveis());
    }
}
