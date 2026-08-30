package br.com.icb.ingressos.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.icb.ingressos.adapter.in.web.dto.CompraResponse;
import br.com.icb.ingressos.adapter.in.web.dto.ComprarIngressoRequest;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoint público de compra de ingresso (RF-04 a RF-10).
 */
@RestController
@RequestMapping("/api/compras")
@Tag(name = "Compra", description = "Compra de ingresso pelo cliente")
public class CompraController {

    private final ComprarIngressoUseCase comprarIngresso;

    public CompraController(ComprarIngressoUseCase comprarIngresso) {
        this.comprarIngresso = comprarIngresso;
    }

    @PostMapping
    @Operation(summary = "Inicia a compra de um ingresso e retorna os dados de pagamento")
    public ResponseEntity<CompraResponse> comprar(@Valid @RequestBody ComprarIngressoRequest requisicao) {
        var resultado = comprarIngresso.comprar(requisicao.toCommand());
        var corpo = CompraResponse.de(resultado);
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + corpo.pedidoId()))
                .body(corpo);
    }
}
