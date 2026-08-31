package br.com.icb.ingressos.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.CadastrarEventoUseCase;
import br.com.icb.ingressos.ports.in.ConsultarInscritosUseCase;
import br.com.icb.ingressos.ports.in.ConsultarInscritosUseCase.Inscrito;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase;
import br.com.icb.ingressos.ports.in.ListarEventosAdminUseCase;
import br.com.icb.ingressos.ports.in.ListarEventosAdminUseCase.EventoAdmin;

@WebMvcTest(AdminEventoController.class)
@AutoConfigureMockMvc(addFilters = false) // autorização coberta em SegurancaAdminTest
class AdminEventoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CadastrarEventoUseCase cadastrarEvento;
    @MockBean private CriarLoteUseCase criarLote;
    @MockBean private ConsultarInscritosUseCase consultarInscritos;
    @MockBean private ListarEventosAdminUseCase listarEventosAdmin;

    @Test
    void listarEventosRetornaTodosComOResumoDosLotes() throws Exception {
        when(listarEventosAdmin.listar()).thenReturn(List.of(
                new EventoAdmin(2L, "Vigília", null, LocalDateTime.of(2026, 11, 1, 20, 0),
                        false, 0, 0, 0),
                new EventoAdmin(1L, "Congresso", "anual", LocalDateTime.of(2026, 10, 1, 19, 0),
                        false, 2, 150, 130)));

        mockMvc.perform(get("/api/admin/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].quantidadeLotes").value(0))
                .andExpect(jsonPath("$[1].nome").value("Congresso"))
                .andExpect(jsonPath("$[1].ingressosDisponiveis").value(130));
    }

    @Test
    void cadastrarEventoRetorna201ComIdELocation() throws Exception {
        when(cadastrarEvento.cadastrar(any())).thenReturn(7L);
        var corpo = """
                { "nome": "Congresso", "descricao": "d", "dataHora": "2026-12-01T20:00:00" }""";

        mockMvc.perform(post("/api/admin/eventos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/eventos/7"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void cadastrarEventoComDataNoPassadoRetorna422() throws Exception {
        var corpo = """
                { "nome": "Congresso", "dataHora": "2020-01-01T20:00:00" }""";

        mockMvc.perform(post("/api/admin/eventos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void criarLoteRetorna201() throws Exception {
        when(criarLote.criar(any())).thenReturn(3L);
        var corpo = """
                { "nome": "Inteira", "preco": 50.00, "quantidadeTotal": 100 }""";

        mockMvc.perform(post("/api/admin/eventos/1/lotes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void criarLoteParaEventoInexistenteRetorna404() throws Exception {
        when(criarLote.criar(any())).thenThrow(new RecursoNaoEncontradoException("Evento", 99L));
        var corpo = """
                { "nome": "Inteira", "preco": 50.00, "quantidadeTotal": 100 }""";

        mockMvc.perform(post("/api/admin/eventos/99/lotes").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarInscritosRetornaAColecao() throws Exception {
        when(consultarInscritos.listarPorEvento(1L)).thenReturn(List.of(new Inscrito(
                "Maria", "maria@exemplo.com", "11999990000", 10L, "Inteira", 30L, "qr-5",
                LocalDateTime.of(2026, 8, 30, 12, 0))));

        mockMvc.perform(get("/api/admin/eventos/1/inscritos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeCliente").value("Maria"))
                .andExpect(jsonPath("$[0].codigoQr").value("qr-5"));
    }
}
