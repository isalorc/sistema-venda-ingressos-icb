package br.com.icb.ingressos.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ResultadoCompra;

@WebMvcTest(CompraController.class)
class CompraControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ComprarIngressoUseCase comprarIngresso;

    private static final String CORPO_VALIDO = """
            { "loteId": 1, "nome": "Maria", "email": "maria@exemplo.com",
              "telefone": "11999990000", "metodoPagamento": "PIX" }""";

    @Test
    void compraComSucessoRetorna201ComOsDadosDePagamento() throws Exception {
        when(comprarIngresso.comprar(any())).thenReturn(new ResultadoCompra(
                10L, 5L, new BigDecimal("50.00"), StatusPedido.PENDENTE, null, "pix-payload"));

        mockMvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").value(10))
                .andExpect(jsonPath("$.statusPedido").value("PENDENTE"))
                .andExpect(jsonPath("$.qrCodePix").value("pix-payload"));
    }

    @Test
    void corpoInvalidoRetorna422ComListaDeCamposESemChamarOCasoDeUso() throws Exception {
        var corpoInvalido = """
                { "loteId": null, "nome": "", "email": "nao-eh-email",
                  "telefone": "11999990000", "metodoPagamento": "PIX" }""";

        mockMvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(corpoInvalido))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.camposInvalidos").isNotEmpty());

        verify(comprarIngresso, never()).comprar(any());
    }

    @Test
    void loteEsgotadoNoCasoDeUsoRetorna409() throws Exception {
        when(comprarIngresso.comprar(any())).thenThrow(new IngressoEsgotadoException(1L));

        mockMvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
