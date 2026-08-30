package br.com.icb.ingressos.usecase;

import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase;
import br.com.icb.ingressos.ports.out.TokenAdminPort;

/**
 * Autentica o administrador da igreja e emite o token de acesso (RF-19 / RN-5).
 *
 * <p>Credencial única, vinda de variáveis de ambiente (RNF-13): e-mail em texto e
 * senha como hash BCrypt. A verificação do hash roda sempre (mesmo com e-mail
 * errado) para não vazar, pelo tempo de resposta, se o e-mail existe.
 */
@Service
public class AutenticarAdminService implements AutenticarAdminUseCase {

    private static final String TIPO_TOKEN = "Bearer";

    private final PasswordEncoder passwordEncoder;
    private final TokenAdminPort tokenAdmin;
    private final String emailAdmin;
    private final String senhaHashAdmin;
    private final Duration expiracao;

    public AutenticarAdminService(PasswordEncoder passwordEncoder,
                                  TokenAdminPort tokenAdmin,
                                  @Value("${app.admin.email}") String emailAdmin,
                                  @Value("${app.admin.senha-hash}") String senhaHashAdmin,
                                  @Value("${app.jwt.expiracao}") Duration expiracao) {
        this.passwordEncoder = passwordEncoder;
        this.tokenAdmin = tokenAdmin;
        this.emailAdmin = emailAdmin;
        this.senhaHashAdmin = senhaHashAdmin;
        this.expiracao = expiracao;
    }

    @Override
    public TokenEmitido autenticar(Credenciais credenciais) {
        var senhaConfere = passwordEncoder.matches(valorOuVazio(credenciais.senha()), senhaHashAdmin);
        var emailConfere = emailAdmin.equalsIgnoreCase(normalizar(credenciais.email()));

        if (!senhaConfere || !emailConfere) {
            throw new CredenciaisInvalidasException();
        }

        return new TokenEmitido(tokenAdmin.gerar(emailAdmin), TIPO_TOKEN, expiracao.toSeconds());
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    private static String valorOuVazio(String valor) {
        return valor == null ? "" : valor;
    }
}
