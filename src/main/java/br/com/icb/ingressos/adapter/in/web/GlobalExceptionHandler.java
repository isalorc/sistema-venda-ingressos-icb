package br.com.icb.ingressos.adapter.in.web;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte exceções não tratadas em respostas HTTP padronizadas ({@link ErroResponse}),
 * sem vazar stack trace para o consumidor da API.
 *
 * <p>Estende {@link ResponseEntityExceptionHandler} para reaproveitar o tratamento
 * das exceções nativas do Spring MVC (rota inexistente, método não suportado,
 * JSON malformado etc.), apenas trocando o corpo da resposta pelo formato padrão.
 *
 * <p>Handlers para as exceções de domínio ({@code IngressoEsgotadoException},
 * {@code RecursoNaoEncontradoException} etc.) são adicionados no Épico 1, quando
 * essas classes passam a existir.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Erros de validação do corpo ({@code @Valid} em {@code @RequestBody}) → 422 com a lista de campos. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException excecao,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest requisicao) {
        var campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::paraCampoInvalido)
                .toList();
        var corpo = ErroResponse.de(HttpStatus.UNPROCESSABLE_ENTITY,
                "Um ou mais campos são inválidos.", caminho(requisicao), campos);
        return handleExceptionInternal(excecao, corpo, headers, HttpStatus.UNPROCESSABLE_ENTITY, requisicao);
    }

    /** Padroniza o corpo de todas as exceções tratadas pela superclasse (404, 405, 415, 400…). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception excecao,
                                                             Object corpo,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest requisicao) {
        var status = resolverStatus(statusCode);
        if (status.is5xxServerError()) {
            log.error("Erro do Spring MVC ao processar {}", caminho(requisicao), excecao);
        }
        var corpoPadronizado = (corpo instanceof ErroResponse erro)
                ? erro
                : ErroResponse.de(status, mensagemPara(status), caminho(requisicao));
        return super.handleExceptionInternal(excecao, corpoPadronizado, headers, statusCode, requisicao);
    }

    /** Validação de parâmetros/query ({@code @Validated} no controller) — não coberta pela superclasse. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(ConstraintViolationException excecao,
                                                                WebRequest requisicao) {
        var campos = excecao.getConstraintViolations().stream()
                .map(violacao -> new ErroResponse.CampoInvalido(
                        violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        return ResponseEntity.unprocessableEntity().body(ErroResponse.de(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Um ou mais parâmetros são inválidos.",
                caminho(requisicao),
                campos));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> tratarArgumentoIlegal(IllegalArgumentException excecao,
                                                              WebRequest requisicao) {
        return ResponseEntity.badRequest().body(ErroResponse.de(
                HttpStatus.BAD_REQUEST,
                excecao.getMessage(),
                caminho(requisicao)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception excecao, WebRequest requisicao) {
        log.error("Erro inesperado ao processar {}", caminho(requisicao), excecao);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErroResponse.de(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.",
                caminho(requisicao)));
    }

    private static ErroResponse.CampoInvalido paraCampoInvalido(FieldError erro) {
        return new ErroResponse.CampoInvalido(erro.getField(), erro.getDefaultMessage());
    }

    private static HttpStatus resolverStatus(HttpStatusCode codigo) {
        var status = HttpStatus.resolve(codigo.value());
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String mensagemPara(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado.";
            case METHOD_NOT_ALLOWED -> "Método HTTP não suportado para este recurso.";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado.";
            case NOT_ACCEPTABLE -> "Nenhuma representação aceitável disponível.";
            case BAD_REQUEST -> "Requisição inválida.";
            default -> status.getReasonPhrase();
        };
    }

    private static String caminho(WebRequest requisicao) {
        if (requisicao instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return "";
    }
}
