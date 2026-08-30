package br.com.icb.ingressos.adapter.in.web.dto;

/**
 * Corpo de resposta dos endpoints que criam um recurso (evento, lote): apenas o
 * id gerado. O caminho do recurso vai no header {@code Location}.
 */
public record RecursoCriadoResponse(Long id) {
}
