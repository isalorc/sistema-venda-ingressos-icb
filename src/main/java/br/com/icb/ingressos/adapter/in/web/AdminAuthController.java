package br.com.icb.ingressos.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.LoginRequest;
import br.com.icb.ingressos.adapter.in.web.dto.TokenResponse;
import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Login do administrador da igreja (RF-19). Rota pública: emite o token usado
 * nas demais rotas {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin/login")
@Tag(name = "Administração", description = "Gestão de eventos, lotes e inscritos (requer login)")
public class AdminAuthController {

    private final AutenticarAdminUseCase autenticarAdmin;

    public AdminAuthController(AutenticarAdminUseCase autenticarAdmin) {
        this.autenticarAdmin = autenticarAdmin;
    }

    @PostMapping
    @Operation(summary = "Autentica o administrador e retorna um token de acesso")
    public TokenResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return TokenResponse.de(autenticarAdmin.autenticar(requisicao.toCredenciais()));
    }
}
