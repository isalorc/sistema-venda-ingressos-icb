package br.com.icb.ingressos.adapter.in.web.dto;

import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase.Credenciais;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da requisição de login do administrador (RF-19).
 */
public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String senha) {

    public Credenciais toCredenciais() {
        return new Credenciais(email, senha);
    }
}
