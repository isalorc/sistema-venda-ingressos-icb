package br.com.icb.ingressos.ports.in;

import java.time.LocalDateTime;

/**
 * Caso de uso administrativo: cadastrar um evento (RF-20).
 *
 * <p>Exige administrador autenticado — a verificação do JWT é responsabilidade
 * do adapter de entrada (RF-26).
 */
public interface CadastrarEventoUseCase {

    /**
     * @return o id do evento criado
     */
    Long cadastrar(CadastrarEventoCommand comando);

    record CadastrarEventoCommand(
            String nome,
            String descricao,
            LocalDateTime dataHora) {
    }
}
