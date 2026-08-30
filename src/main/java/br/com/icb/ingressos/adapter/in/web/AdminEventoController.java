package br.com.icb.ingressos.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.CadastrarEventoRequest;
import br.com.icb.ingressos.adapter.in.web.dto.CriarLoteRequest;
import br.com.icb.ingressos.adapter.in.web.dto.InscritoResponse;
import br.com.icb.ingressos.adapter.in.web.dto.RecursoCriadoResponse;
import br.com.icb.ingressos.ports.in.CadastrarEventoUseCase;
import br.com.icb.ingressos.ports.in.ConsultarInscritosUseCase;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints administrativos de gestão de eventos, lotes e inscritos
 * (RF-20, RF-22, RF-23, RF-25).
 *
 * <p><strong>TODO Épico 8:</strong> estas rotas exigem administrador autenticado
 * via JWT (RF-26 / RNF-12). No MVP navegável elas ficam abertas — a segurança
 * entra junto com o {@code POST /api/admin/login}.
 */
@RestController
@RequestMapping("/api/admin/eventos")
@Tag(name = "Administração", description = "Gestão de eventos, lotes e inscritos (autenticação: Épico 8)")
public class AdminEventoController {

    private final CadastrarEventoUseCase cadastrarEvento;
    private final CriarLoteUseCase criarLote;
    private final ConsultarInscritosUseCase consultarInscritos;

    public AdminEventoController(CadastrarEventoUseCase cadastrarEvento,
                                CriarLoteUseCase criarLote,
                                ConsultarInscritosUseCase consultarInscritos) {
        this.cadastrarEvento = cadastrarEvento;
        this.criarLote = criarLote;
        this.consultarInscritos = consultarInscritos;
    }

    @PostMapping
    @Operation(summary = "Cadastra um evento")
    public ResponseEntity<RecursoCriadoResponse> cadastrarEvento(
            @Valid @RequestBody CadastrarEventoRequest requisicao) {
        var eventoId = cadastrarEvento.cadastrar(requisicao.toCommand());
        return ResponseEntity
                .created(URI.create("/api/eventos/" + eventoId))
                .body(new RecursoCriadoResponse(eventoId));
    }

    @PostMapping("/{eventoId}/lotes")
    @Operation(summary = "Cria um lote de ingressos para um evento")
    public ResponseEntity<RecursoCriadoResponse> criarLote(
            @PathVariable Long eventoId,
            @Valid @RequestBody CriarLoteRequest requisicao) {
        var loteId = criarLote.criar(requisicao.toCommand(eventoId));
        return ResponseEntity
                .created(URI.create("/api/eventos/" + eventoId))
                .body(new RecursoCriadoResponse(loteId));
    }

    @GetMapping("/{eventoId}/inscritos")
    @Operation(summary = "Lista os inscritos (pedidos pagos) de um evento")
    public List<InscritoResponse> listarInscritos(@PathVariable Long eventoId) {
        return consultarInscritos.listarPorEvento(eventoId).stream()
                .map(InscritoResponse::de)
                .toList();
    }
}
