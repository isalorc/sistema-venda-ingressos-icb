package br.com.icb.ingressos.adapter.in.web;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

/**
 * Corpo de resposta padrão para erros da API.
 *
 * @param timestamp       momento em que o erro foi gerado
 * @param status          código HTTP
 * @param erro            frase do status HTTP (ex.: "Not Found")
 * @param mensagem        mensagem legível para o consumidor da API
 * @param caminho         URI da requisição que falhou
 * @param camposInvalidos detalhes de validação (omitido quando vazio)
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> camposInvalidos
) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, caminho, List.of());
    }

    public static ErroResponse de(HttpStatus status, String mensagem, String caminho, List<CampoInvalido> camposInvalidos) {
        return new ErroResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, caminho, camposInvalidos);
    }
}
