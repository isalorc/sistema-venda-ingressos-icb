package br.com.icb.ingressos.domain;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @EqualsAndHashCode.Include
    private final Long id;
    private final String nome;
    private final String email;
    private final String telefone;
    private final LocalDateTime dataCriacao;

    private Usuario(Long id, String nome, String email, String telefone, LocalDateTime dataCriacao) {
        this.id = id;
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.email = normalizarEmail(email);
        this.telefone = Validacao.exigirTexto(telefone, "telefone");
        this.dataCriacao = Validacao.exigir(dataCriacao, "dataCriacao");
    }

    public static Usuario novo(String nome, String email, String telefone, LocalDateTime dataCriacao) {
        return new Usuario(null, nome, email, telefone, dataCriacao);
    }

    public static Usuario reconstituir(Long id, String nome, String email, String telefone,
                                       LocalDateTime dataCriacao) {
        return new Usuario(Validacao.exigir(id, "id"), nome, email, telefone, dataCriacao);
    }

    private static String normalizarEmail(String email) {
        var normalizado = Validacao.exigirTexto(email, "email").toLowerCase();
        if (!EMAIL.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("email inválido.");
        }
        return normalizado;
    }
}
