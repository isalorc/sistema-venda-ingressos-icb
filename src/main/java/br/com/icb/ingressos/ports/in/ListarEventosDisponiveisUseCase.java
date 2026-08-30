package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: listar os eventos disponíveis para compra (RF-01).
 *
 * <p>Um evento é considerado disponível quando ainda não ocorreu e tem pelo
 * menos um lote com ingressos disponíveis (RF-03).
 */
public interface ListarEventosDisponiveisUseCase {

    List<EventoDisponivel> listar();

    /**
     * Resumo de um evento para a tela de listagem.
     *
     * @param menorPreco            menor preço entre os lotes com disponibilidade
     * @param ingressosDisponiveis  soma da quantidade disponível dos lotes
     */
    record EventoDisponivel(
            Long eventoId,
            String nome,
            String descricao,
            LocalDateTime dataHora,
            BigDecimal menorPreco,
            int ingressosDisponiveis) {
    }
}
