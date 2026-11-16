package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.ports.in.ContaClienteUseCase.Cadastro;
import br.com.icb.ingressos.ports.in.ContaClienteUseCase.Credenciais;
import br.com.icb.ingressos.ports.out.TokenClientePort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ContaClienteServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private TokenClientePort tokenCliente;

    private ContaClienteService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ContaClienteService(usuarioRepository, ENCODER, tokenCliente, clock, Duration.ofDays(7));
    }

    private static Cadastro cadastro() {
        return new Cadastro("Maria", "Maria@Email.com ", "11999990000", "senha-forte");
    }

    private void salvarDevolvendoComId() {
        when(usuarioRepository.salvar(any())).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return Usuario.reconstituir(7L, u.getNome(), u.getEmail(), u.getTelefone(), u.getSenhaHash(),
                    u.getDataCriacao());
        });
    }

    @Test
    void cadastroNovoCriaContaComSenhaEmHashEDevolveToken() {
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.empty());
        salvarDevolvendoComId();
        when(tokenCliente.gerar(7L)).thenReturn("token-7");

        var token = service.cadastrar(cadastro());

        assertThat(token.token()).isEqualTo("token-7");
        assertThat(token.tipo()).isEqualTo("Bearer");
        assertThat(token.expiraEmSegundos()).isEqualTo(Duration.ofDays(7).toSeconds());
        var salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).salvar(salvo.capture());
        assertThat(salvo.getValue().possuiConta()).isTrue();
        assertThat(salvo.getValue().getSenhaHash()).isNotEqualTo("senha-forte");
        assertThat(ENCODER.matches("senha-forte", salvo.getValue().getSenhaHash())).isTrue();
    }

    @Test
    void cadastroComEmailDeCompraAntigaAtivaAContaDesseUsuario() {
        var semConta = Usuario.reconstituir(3L, "M.", "maria@email.com", "000", AGORA.minusDays(30));
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.of(semConta));
        when(usuarioRepository.salvar(semConta)).thenReturn(semConta);
        when(tokenCliente.gerar(3L)).thenReturn("token-3");

        var token = service.cadastrar(cadastro());

        assertThat(token.token()).isEqualTo("token-3");
        assertThat(semConta.possuiConta()).isTrue();
        assertThat(semConta.getNome()).isEqualTo("Maria");
        assertThat(semConta.getTelefone()).isEqualTo("11999990000");
    }

    @Test
    void cadastroComEmailQueJaTemContaRetornaConflito() {
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.of(
                Usuario.reconstituir(3L, "Maria", "maria@email.com", "000", "hash", AGORA)));

        assertThatExceptionOfType(EmailJaCadastradoException.class)
                .isThrownBy(() -> service.cadastrar(cadastro()));

        verify(usuarioRepository, never()).salvar(any());
    }

    @Test
    void telefoneComMascaraEhGuardadoSoComDigitos() {
        when(usuarioRepository.buscarPorEmail("ana@email.com")).thenReturn(Optional.empty());
        salvarDevolvendoComId();
        when(tokenCliente.gerar(7L)).thenReturn("token-7");

        service.cadastrar(new Cadastro("Ana", "ana@email.com", "(13) 99999-8888", "senha-forte"));

        var salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).salvar(salvo.capture());
        assertThat(salvo.getValue().getTelefone()).isEqualTo("13999998888");
    }

    @Test
    void telefoneInvalidoRecusaOCadastro() {
        when(usuarioRepository.buscarPorEmail("ana@email.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.cadastrar(new Cadastro("Ana", "ana@email.com", "12345", "senha-forte")))
                .withMessageContaining("Telefone inválido");

        verify(usuarioRepository, never()).salvar(any());
    }

    @Test
    void senhaCurtaRecusaOCadastro() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.cadastrar(new Cadastro("Maria", "m@e.com", "1", "curta")))
                .withMessageContaining("8 caracteres");
    }

    @Test
    void loginComSenhaCertaDevolveToken() {
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.of(
                Usuario.reconstituir(7L, "Maria", "maria@email.com", "000", ENCODER.encode("senha-forte"), AGORA)));
        when(tokenCliente.gerar(7L)).thenReturn("token-7");

        assertThat(service.autenticar(new Credenciais(" MARIA@email.com", "senha-forte")).token())
                .isEqualTo("token-7");
    }

    @Test
    void loginComSenhaErradaFalhaComMensagemGenerica() {
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.of(
                Usuario.reconstituir(7L, "Maria", "maria@email.com", "000", ENCODER.encode("senha-forte"), AGORA)));

        assertThatExceptionOfType(CredenciaisInvalidasException.class)
                .isThrownBy(() -> service.autenticar(new Credenciais("maria@email.com", "errada")));
    }

    @Test
    void loginDeUsuarioSemContaFalha() {
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.of(
                Usuario.reconstituir(7L, "Maria", "maria@email.com", "000", AGORA)));

        assertThatExceptionOfType(CredenciaisInvalidasException.class)
                .isThrownBy(() -> service.autenticar(new Credenciais("maria@email.com", "qualquer")));
    }

    @Test
    void loginDeEmailInexistenteFalha() {
        when(usuarioRepository.buscarPorEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(CredenciaisInvalidasException.class)
                .isThrownBy(() -> service.autenticar(new Credenciais("ninguem@email.com", "x")));
    }
}
