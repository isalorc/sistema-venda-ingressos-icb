package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Casos de uso administrativos de gestão de um lote já criado: editar e excluir
 * (ver {@code docs/gestaoDeEventos.md}). Exige administrador autenticado.
 */
public interface GerenciarLoteUseCase {

    /**
     * Substitui os dados editáveis do lote. {@code quantidadeTotal} só pode
     * aumentar — o acréscimo vira estoque disponível.
     *
     * @throws br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException se o lote não existir no evento
     * @throws br.com.icb.ingressos.domain.exception.TransicaoInvalidaException se o evento estiver cancelado ou se tentar reduzir a capacidade
     * @throws br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException se a janela de vendas for inconsistente
     */
    void editar(Long eventoId, Long loteId, DadosDoLote dados);

    /**
     * Exclui o lote e seus ingressos em cascata.
     *
     * @throws br.com.icb.ingressos.domain.exception.TransicaoInvalidaException
     *         se houver ingresso reservado ou vendido no lote
     */
    void excluir(Long eventoId, Long loteId);

    record DadosDoLote(
            String nome,
            BigDecimal preco,
            int quantidadeTotal,
            LocalDateTime inicioVendas,
            LocalDateTime fimVendas) {
    }
}
