package br.com.icb.ingressos.adapter.in.web.dto;

import br.com.icb.ingressos.ports.in.ContaClienteUseCase.Cadastro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
        @NotBlank(message = "O nome é obrigatório.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "O telefone é obrigatório.")
        @Pattern(regexp = "^(\\+?55[\\s-]?)?\\(?[1-9]{2}\\)?[\\s-]?(9\\d{4}|[2-8]\\d{3})[\\s-]?\\d{4}$",
                message = "Telefone inválido. Informe DDD + número, ex.: (13) 99999-8888.")
        String telefone,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha) {

    public Cadastro toCadastro() {
        return new Cadastro(nome, email, telefone, senha);
    }
}
