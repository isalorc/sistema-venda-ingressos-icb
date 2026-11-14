package br.com.icb.ingressos.adapter.in.web.dto;

import br.com.icb.ingressos.ports.in.ContaClienteUseCase.Cadastro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "O telefone é obrigatório.")
        String telefone,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha) {

    public Cadastro toCadastro() {
        return new Cadastro(nome, email, telefone, senha);
    }
}
