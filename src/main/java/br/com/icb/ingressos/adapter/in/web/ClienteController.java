package br.com.icb.ingressos.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.CadastroClienteRequest;
import br.com.icb.ingressos.adapter.in.web.dto.LoginRequest;
import br.com.icb.ingressos.adapter.in.web.dto.PedidoClienteDetalheResponse;
import br.com.icb.ingressos.adapter.in.web.dto.PedidoClienteResponse;
import br.com.icb.ingressos.adapter.in.web.dto.PerfilClienteResponse;
import br.com.icb.ingressos.adapter.in.web.dto.TokenResponse;
import br.com.icb.ingressos.ports.in.ConsultarPedidosClienteUseCase;
import br.com.icb.ingressos.ports.in.ContaClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Área do cliente: cadastro, login, perfil e "Meus pedidos". Cadastro e login são
 * públicos; o resto exige token de cliente (principal = id do cliente).
 */
@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Cliente", description = "Conta do cliente e acompanhamento de pedidos")
public class ClienteController {

    private final ContaClienteUseCase contaCliente;
    private final ConsultarPedidosClienteUseCase pedidosCliente;

    public ClienteController(ContaClienteUseCase contaCliente,
                             ConsultarPedidosClienteUseCase pedidosCliente) {
        this.contaCliente = contaCliente;
        this.pedidosCliente = pedidosCliente;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria a conta do cliente e já devolve o token de acesso")
    public TokenResponse cadastrar(@Valid @RequestBody CadastroClienteRequest requisicao) {
        return TokenResponse.de(contaCliente.cadastrar(requisicao.toCadastro()));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o cliente e devolve o token de acesso")
    public TokenResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return TokenResponse.de(contaCliente.autenticar(
                new ContaClienteUseCase.Credenciais(requisicao.email(), requisicao.senha())));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do cliente logado")
    public PerfilClienteResponse perfil(@AuthenticationPrincipal Long usuarioId) {
        return PerfilClienteResponse.de(contaCliente.perfil(usuarioId));
    }

    @GetMapping("/me/pedidos")
    @Operation(summary = "Pedidos do cliente logado, mais recentes primeiro")
    public List<PedidoClienteResponse> pedidos(@AuthenticationPrincipal Long usuarioId) {
        return pedidosCliente.listar(usuarioId).stream().map(PedidoClienteResponse::de).toList();
    }

    @GetMapping("/me/pedidos/{pedidoId}")
    @Operation(summary = "Detalhe de um pedido do cliente logado, com ingressos e QR")
    public PedidoClienteDetalheResponse pedido(@AuthenticationPrincipal Long usuarioId,
                                               @PathVariable Long pedidoId) {
        return PedidoClienteDetalheResponse.de(pedidosCliente.detalhar(usuarioId, pedidoId));
    }
}
