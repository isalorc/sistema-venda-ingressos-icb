package br.com.icb.ingressos.ports.in;

import java.math.BigDecimal;

/**
 * Caso de uso administrativo: criar um lote de ingressos para um evento
 * (RF-22, RF-23).
 *
 * <p>Ao criar o lote, o sistema gera {@code quantidadeTotal} ingressos com
 * status {@code DISPONIVEL} (RF-23). Exige administrador autenticado (RF-26).
 */
public interface CriarLoteUseCase {

    /**
     * @return o id do lote criado
     * @throws br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException
     *         se o evento não existir
     */
    Long criar(CriarLoteCommand comando);

    record CriarLoteCommand(
            Long eventoId,
            String nome,
            BigDecimal preco,
            int quantidadeTotal) {
    }
}
