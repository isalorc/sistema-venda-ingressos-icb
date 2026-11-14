package br.com.icb.ingressos.domain;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Cliente que compra ingressos (RN-4).
 *
 * <p>Desde a área do cliente, comprar exige conta: {@code senhaHash} guarda o hash
 * BCrypt da senha. Usuários criados antes disso (compras sem login) têm
 * {@code senhaHash} nulo — ainda não possuem conta — e podem ativá-la cadastrando-se
 * com o mesmo e-mail ({@link #ativarConta}).
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @EqualsAndHashCode.Include
    private final Long id;
    private String nome;
    private final String email;
    private String telefone;
    private String senhaHash;
    private final LocalDateTime dataCriacao;

    private Usuario(Long id, String nome, String email, String telefone, String senhaHash,
                    LocalDateTime dataCriacao) {
        this.id = id;
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.email = normalizarEmail(email);
        this.telefone = Validacao.exigirTexto(telefone, "telefone");
        this.senhaHash = senhaHash;
        this.dataCriacao = Validacao.exigir(dataCriacao, "dataCriacao");
    }

    /** Usuário sem conta (fluxo antigo de compra sem login). */
    public static Usuario novo(String nome, String email, String telefone, LocalDateTime dataCriacao) {
        return new Usuario(null, nome, email, telefone, null, dataCriacao);
    }

    /** Cliente com conta (cadastro na área do cliente). */
    public static Usuario comConta(String nome, String email, String telefone, String senhaHash,
                                   LocalDateTime dataCriacao) {
        return new Usuario(null, nome, email, telefone,
                Validacao.exigirTexto(senhaHash, "senhaHash"), dataCriacao);
    }

    public static Usuario reconstituir(Long id, String nome, String email, String telefone,
                                       LocalDateTime dataCriacao) {
        return reconstituir(id, nome, email, telefone, null, dataCriacao);
    }

    public static Usuario reconstituir(Long id, String nome, String email, String telefone,
                                       String senhaHash, LocalDateTime dataCriacao) {
        return new Usuario(Validacao.exigir(id, "id"), nome, email, telefone, senhaHash, dataCriacao);
    }

    public boolean possuiConta() {
        return senhaHash != null;
    }

    /**
     * Transforma um usuário sem conta (comprou antes do login existir) em cliente
     * com conta, atualizando nome e telefone para os informados no cadastro.
     */
    public void ativarConta(String nome, String telefone, String senhaHash) {
        if (possuiConta()) {
            throw new TransicaoInvalidaException("Já existe uma conta com este e-mail.");
        }
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.telefone = Validacao.exigirTexto(telefone, "telefone");
        this.senhaHash = Validacao.exigirTexto(senhaHash, "senhaHash");
    }

    private static String normalizarEmail(String email) {
        var normalizado = Validacao.exigirTexto(email, "email").toLowerCase();
        if (!EMAIL.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("email inválido.");
        }
        return normalizado;
    }
}
