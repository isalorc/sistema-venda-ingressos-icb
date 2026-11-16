package br.com.icb.ingressos.ports.in;

import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.domain.enums.StatusVendas;

/**
 * Caso de uso: listar <strong>todos</strong> os eventos para o painel
 * administrativo (RF-21) — inclusive os que ainda não têm lote e os que já
 * ocorreram. Diferente de {@link ListarEventosDisponiveisUseCase}, que só
 * devolve o que o cliente pode comprar.
 *
 * <p>Cada item traz um resumo dos lotes para a tela de gestão distinguir de
 * relance um evento vazio de um com ingressos, e o {@link StatusVendas} para
 * saber se o evento está vendendo, agendado ou encerrado.
 */
public interface ListarEventosAdminUseCase {

    List<EventoAdmin> listar();

    /**
     * @param jaOcorreu             a data do evento já passou
     * @param quantidadeLotes       lotes cadastrados (0 = evento sem ingressos)
     * @param ingressosTotais       soma da capacidade de todos os lotes
     * @param ingressosDisponiveis  soma do estoque ainda livre de todos os lotes
     * @param statusVendas          {@code A_VENDA}, {@code AGENDADA} ou {@code ENCERRADA}
     */
    record EventoAdmin(
            Long id,
            String nome,
            String descricao,
            LocalDateTime dataHora,
            LocalDateTime dataFim,
            String imagemUrl,
            boolean cancelado,
            boolean jaOcorreu,
            int quantidadeLotes,
            int ingressosTotais,
            int ingressosDisponiveis,
            StatusVendas statusVendas) {
    }
}
