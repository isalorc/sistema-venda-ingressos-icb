package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import br.com.icb.ingressos.domain.enums.StatusVendas;

/**
 * Caso de uso: listar os eventos disponíveis para compra (RF-01).
 *
 * <p>Um evento entra na lista quando ainda não ocorreu e suas vendas não estão
 * encerradas (há um lote à venda ou agendado). O preço e a disponibilidade
 * refletem o lote ativo — ou, se nenhum está ativo, o próximo a abrir. Ver
 * {@code docs/viradaDeLote.md}.
 */
public interface ListarEventosDisponiveisUseCase {

    List<EventoDisponivel> listar();

    /**
     * Resumo de um evento para a tela de listagem.
     *
     * @param menorPreco            preço do lote à venda (ou do próximo a abrir)
     * @param ingressosDisponiveis  estoque do lote à venda (ou do próximo a abrir)
     * @param statusVendas          {@code A_VENDA}, {@code AGENDADA} ou {@code ENCERRADA}
     * @param aberturaVendas        data em que o próximo lote abre; nulo fora de {@code AGENDADA}
     */
    record EventoDisponivel(
            Long eventoId,
            String nome,
            String descricao,
            LocalDateTime dataHora,
            LocalDateTime dataFim,
            String imagemUrl,
            BigDecimal menorPreco,
            int ingressosDisponiveis,
            StatusVendas statusVendas,
            LocalDateTime aberturaVendas) {
    }
}
