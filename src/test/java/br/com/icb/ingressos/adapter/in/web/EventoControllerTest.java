package br.com.icb.ingressos.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase.DetalheEvento;
import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase;
import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase.EventoDisponivel;

@WebMvcTest(EventoController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ListarEventosDisponiveisUseCase listarEventosDisponiveis;
    @MockBean private ConsultarEventoUseCase consultarEvento;

    @Test
    void listaEventosDisponiveis() throws Exception {
        when(listarEventosDisponiveis.listar()).thenReturn(List.of(new EventoDisponivel(
                1L, "Congresso", "descrição", LocalDateTime.of(2026, 12, 1, 20, 0),
                new BigDecimal("50.00"), 8)));

        mockMvc.perform(get("/api/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].menorPreco").value(50.00))
                .andExpect(jsonPath("$[0].ingressosDisponiveis").value(8));
    }

    @Test
    void detalhaEvento() throws Exception {
        when(consultarEvento.consultar(1L)).thenReturn(new DetalheEvento(
                1L, "Congresso", "descrição", LocalDateTime.of(2026, 12, 1, 20, 0),
                List.of(new DetalheEvento.LoteDisponivel(10L, "Inteira", new BigDecimal("50.00"), 40))));

        mockMvc.perform(get("/api/eventos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Congresso"))
                .andExpect(jsonPath("$.lotes[0].quantidadeDisponivel").value(40));
    }

    @Test
    void eventoInexistenteRetorna404NoFormatoPadrao() throws Exception {
        when(consultarEvento.consultar(eq(99L)))
                .thenThrow(new RecursoNaoEncontradoException("Evento", 99L));

        mockMvc.perform(get("/api/eventos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.caminho").value("/api/eventos/99"));
    }
}
