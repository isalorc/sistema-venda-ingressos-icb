package br.com.icb.ingressos.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ContaClienteUseCase;
import br.com.icb.ingressos.ports.out.TokenClientePort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@Service
public class ContaClienteService implements ContaClienteUseCase {

    private static final String TIPO_TOKEN = "Bearer";
    static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenClientePort tokenCliente;
    private final Clock clock;
    private final Duration expiracao;

    public ContaClienteService(UsuarioRepositoryPort usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               TokenClientePort tokenCliente,
                               Clock clock,
                               @Value("${app.cliente.token-expiracao}") Duration expiracao) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenCliente = tokenCliente;
        this.clock = clock;
        this.expiracao = expiracao;
    }

    @Override
    @Transactional
    public TokenCliente cadastrar(Cadastro cadastro) {
        var senha = cadastro.senha() == null ? "" : cadastro.senha();
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                    "A senha deve ter pelo menos %d caracteres.".formatted(TAMANHO_MINIMO_SENHA));
        }
        var hash = passwordEncoder.encode(senha);
        var existente = usuarioRepository.buscarPorEmail(normalizar(cadastro.email()));
        Usuario cliente;
        if (existente.isPresent()) {
            cliente = existente.get();
            if (cliente.possuiConta()) {
                throw new EmailJaCadastradoException();
            }
            cliente.ativarConta(cadastro.nome(), cadastro.telefone(), hash);
        } else {
            cliente = Usuario.comConta(cadastro.nome(), cadastro.email(), cadastro.telefone(), hash,
                    LocalDateTime.now(clock));
        }
        return emitir(usuarioRepository.salvar(cliente));
    }

    @Override
    @Transactional(readOnly = true)
    public TokenCliente autenticar(Credenciais credenciais) {
        var senha = credenciais.senha() == null ? "" : credenciais.senha();
        return usuarioRepository.buscarPorEmail(normalizar(credenciais.email()))
                .filter(Usuario::possuiConta)
                .filter(cliente -> passwordEncoder.matches(senha, cliente.getSenhaHash()))
                .map(this::emitir)
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Perfil perfil(Long usuarioId) {
        var cliente = usuarioRepository.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", usuarioId));
        return new Perfil(cliente.getId(), cliente.getNome(), cliente.getEmail(), cliente.getTelefone());
    }

    private TokenCliente emitir(Usuario cliente) {
        return new TokenCliente(tokenCliente.gerar(cliente.getId()), TIPO_TOKEN, expiracao.toSeconds());
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
