package br.com.icb.ingressos.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.CadastrarEventoRequest;
import br.com.icb.ingressos.adapter.in.web.dto.CriarLoteRequest;
import br.com.icb.ingressos.adapter.in.web.dto.EditarEventoRequest;
import br.com.icb.ingressos.adapter.in.web.dto.EditarLoteRequest;
import br.com.icb.ingressos.adapter.in.web.dto.EventoAdminResponse;
import br.com.icb.ingressos.adapter.in.web.dto.EventoDetalheResponse;
import br.com.icb.ingressos.adapter.in.web.dto.InscritoResponse;
import br.com.icb.ingressos.adapter.in.web.dto.RecursoCriadoResponse;
import br.com.icb.ingressos.ports.in.CadastrarEventoUseCase;
import br.com.icb.ingressos.ports.in.ConsultarEventoUseCase;
import br.com.icb.ingressos.ports.in.ConsultarInscritosUseCase;
import br.com.icb.ingressos.ports.in.CriarLoteUseCase;
import br.com.icb.ingressos.ports.in.GerenciarEventoUseCase;
import br.com.icb.ingressos.ports.in.GerenciarLoteUseCase;
import br.com.icb.ingressos.ports.in.ListarEventosAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints administrativos de gestão de eventos, lotes e inscritos
 * (RF-20 a RF-25 + {@code docs/gestaoDeEventos.md}).
 *
 * <p>Todas as rotas exigem administrador autenticado (RF-26): envie o token do
 * {@code POST /api/admin/login} no header {@code Authorization: Bearer <token>}.
 */
@RestController
@RequestMapping("/api/admin/eventos")
@Tag(name = "Administração", description = "Gestão de eventos, lotes e inscritos (requer login)")
public class AdminEventoController {

    private final CadastrarEventoUseCase cadastrarEvento;
    private final CriarLoteUseCase criarLote;
    private final ConsultarInscritosUseCase consultarInscritos;
    private final ListarEventosAdminUseCase listarEventosAdmin;
    private final GerenciarEventoUseCase gerenciarEvento;
    private final GerenciarLoteUseCase gerenciarLote;
    private final ConsultarEventoUseCase consultarEvento;

    public AdminEventoController(CadastrarEventoUseCase cadastrarEvento,
                                CriarLoteUseCase criarLote,
                                ConsultarInscritosUseCase consultarInscritos,
                                ListarEventosAdminUseCase listarEventosAdmin,
                                GerenciarEventoUseCase gerenciarEvento,
                                GerenciarLoteUseCase gerenciarLote,
                                ConsultarEventoUseCase consultarEvento) {
        this.cadastrarEvento = cadastrarEvento;
        this.criarLote = criarLote;
        this.consultarInscritos = consultarInscritos;
        this.listarEventosAdmin = listarEventosAdmin;
        this.gerenciarEvento = gerenciarEvento;
        this.gerenciarLote = gerenciarLote;
        this.consultarEvento = consultarEvento;
    }

    @GetMapping
    @Operation(summary = "Lista todos os eventos (inclusive sem lote, cancelados e já ocorridos)")
    public List<EventoAdminResponse> listarEventos() {
        return listarEventosAdmin.listar().stream()
                .map(EventoAdminResponse::de)
                .toList();
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

    @PutMapping("/{eventoId}")
    @Operation(summary = "Edita os dados de um evento")
    public EventoDetalheResponse editarEvento(@PathVariable Long eventoId,
                                              @Valid @RequestBody EditarEventoRequest requisicao) {
        gerenciarEvento.editar(eventoId, requisicao.toDados());
        return EventoDetalheResponse.de(consultarEvento.consultar(eventoId));
    }

    @PostMapping("/{eventoId}/cancelamento")
    @Operation(summary = "Cancela um evento (some da vitrine; histórico preservado)")
    public ResponseEntity<Void> cancelarEvento(@PathVariable Long eventoId) {
        gerenciarEvento.cancelar(eventoId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventoId}")
    @Operation(summary = "Exclui um evento (só sem ingresso vendido ou reservado)")
    public ResponseEntity<Void> excluirEvento(@PathVariable Long eventoId) {
        gerenciarEvento.excluir(eventoId);
        return ResponseEntity.noContent().build();
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

    @PutMapping("/{eventoId}/lotes/{loteId}")
    @Operation(summary = "Edita um lote (quantidade só pode aumentar)")
    public EventoDetalheResponse editarLote(@PathVariable Long eventoId,
                                            @PathVariable Long loteId,
                                            @Valid @RequestBody EditarLoteRequest requisicao) {
        gerenciarLote.editar(eventoId, loteId, requisicao.toDados());
        return EventoDetalheResponse.de(consultarEvento.consultar(eventoId));
    }

    @DeleteMapping("/{eventoId}/lotes/{loteId}")
    @Operation(summary = "Exclui um lote (só sem ingresso vendido ou reservado)")
    public ResponseEntity<Void> excluirLote(@PathVariable Long eventoId, @PathVariable Long loteId) {
        gerenciarLote.excluir(eventoId, loteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventoId}/inscritos")
    @Operation(summary = "Lista os inscritos (pedidos pagos) de um evento")
    public List<InscritoResponse> listarInscritos(@PathVariable Long eventoId) {
        return consultarInscritos.listarPorEvento(eventoId).stream()
                .map(InscritoResponse::de)
                .toList();
    }
}
