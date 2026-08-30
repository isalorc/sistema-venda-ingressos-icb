package br.com.icb.ingressos.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.EventoDetalheResponse;
import br.com.icb.ingressos.adapter.in.web.dto.EventoResumoResponse;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase;
import br.com.icb.ingressos.ports.in.ListarEventosDisponiveisUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints públicos de consulta de eventos pelo cliente (RF-01, RF-02).
 */
@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Consulta pública de eventos e lotes")
public class EventoController {

    private final ListarEventosDisponiveisUseCase listarEventosDisponiveis;
    private final ConsultarEventoUseCase consultarEvento;

    public EventoController(ListarEventosDisponiveisUseCase listarEventosDisponiveis,
                            ConsultarEventoUseCase consultarEvento) {
        this.listarEventosDisponiveis = listarEventosDisponiveis;
        this.consultarEvento = consultarEvento;
    }

    @GetMapping
    @Operation(summary = "Lista os eventos disponíveis para compra")
    public List<EventoResumoResponse> listar() {
        return listarEventosDisponiveis.listar().stream()
                .map(EventoResumoResponse::de)
                .toList();
    }

    @GetMapping("/{eventoId}")
    @Operation(summary = "Detalha um evento e seus lotes")
    public EventoDetalheResponse consultar(@PathVariable Long eventoId) {
        return EventoDetalheResponse.de(consultarEvento.consultar(eventoId));
    }
}
