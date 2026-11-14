package br.com.icb.ingressos.adapter.in.web.dto;

import br.com.icb.ingressos.ports.in.ContaClienteUseCase.Perfil;

public record PerfilClienteResponse(Long id, String nome, String email, String telefone) {

    public static PerfilClienteResponse de(Perfil perfil) {
        return new PerfilClienteResponse(perfil.id(), perfil.nome(), perfil.email(), perfil.telefone());
    }
}
