package br.com.icb.ingressos.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase;
import br.com.icb.ingressos.ports.in.AutenticarAdminUseCase.TokenEmitido;
import br.com.icb.ingressos.usecase.CredenciaisInvalidasException;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AutenticarAdminUseCase autenticarAdmin;

    private static final String CORPO = """
            { "email": "admin@icb.local", "senha": "%s" }""";

    @Test
    void loginComSucessoRetornaOToken() throws Exception {
        when(autenticarAdmin.autenticar(any())).thenReturn(new TokenEmitido("jwt-abc", "Bearer", 7200));

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted("admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-abc"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(7200));
    }

    @Test
    void credencialInvalidaRetorna401() throws Exception {
        when(autenticarAdmin.autenticar(any())).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO.formatted("errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
    }

    @Test
    void corpoInvalidoRetorna422() throws Exception {
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"\", \"senha\": \"\" }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.camposInvalidos").isNotEmpty());
    }
}
