package br.com.icb.ingressos.ports.in;

import java.time.LocalDateTime;

/**
 * Casos de uso administrativos de gestão de um evento já criado: editar,
 * cancelar e excluir (ver {@code docs/gestaoDeEventos.md}). Exige administrador
 * autenticado (RF-26 — no adapter).
 */
public interface GerenciarEventoUseCase {

    /**
     * Substitui os dados editáveis do evento.
     *
     * @throws br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException se o evento não existir
     * @throws br.com.icb.ingressos.domain.exception.TransicaoInvalidaException se o evento estiver cancelado
     * @throws br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException
     *         se a nova data deixar a janela de vendas de algum lote inválida
     */
    void editar(Long eventoId, DadosDoEvento dados);

    /** Marca o evento como cancelado. Idempotente. */
    void cancelar(Long eventoId);

    /**
     * Exclui o evento, seus lotes e ingressos em cascata.
     *
     * @throws br.com.icb.ingressos.domain.exception.TransicaoInvalidaException
     *         se houver ingresso reservado ou vendido no evento
     */
    void excluir(Long eventoId);

    record DadosDoEvento(
            String nome,
            String descricao,
            LocalDateTime dataHora,
            LocalDateTime dataFim,
            String imagemUrl) {
    }
}
