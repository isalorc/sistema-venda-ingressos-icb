package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Caso de uso administrativo: criar um lote de ingressos para um evento
 * (RF-22, RF-23).
 *
 * <p>Ao criar o lote, o sistema gera {@code quantidadeTotal} ingressos com
 * status {@code DISPONIVEL} (RF-23). Exige administrador autenticado (RF-26).
 *
 * <p>A janela de vendas ({@code inicioVendas} / {@code fimVendas}) é opcional e
 * governa a "virada de lote" — ver {@code docs/viradaDeLote.md}.
 */
public interface CriarLoteUseCase {

    /**
     * @return o id do lote criado
     * @throws br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException
     *         se o evento não existir
     * @throws br.com.icb.ingressos.domain.exception.PeriodoDeVendaInvalidoException
     *         se a janela de vendas for inconsistente
     */
    Long criar(CriarLoteCommand comando);

    /**
     * @param inicioVendas abertura das vendas do lote (opcional; nulo = já aberto)
     * @param fimVendas     fim das vendas do lote (opcional; nulo = sem prazo)
     */
    record CriarLoteCommand(
            Long eventoId,
            String nome,
            BigDecimal preco,
            int quantidadeTotal,
            LocalDateTime inicioVendas,
            LocalDateTime fimVendas) {
    }
}
