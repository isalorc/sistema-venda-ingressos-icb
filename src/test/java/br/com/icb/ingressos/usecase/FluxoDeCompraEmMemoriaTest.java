package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ComprarIngressoCommand;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.NotificacaoPagamento;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.ResultadoPagamento;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

/**
 * Integração dos Épicos 3 + 5 + 6.1: os casos de uso reais sobre os repositórios
 * em memória e o gateway <em>fake</em>, exercitando o fluxo compra &rarr; webhook.
 */
@SpringBootTest
class FluxoDeCompraEmMemoriaTest {

    @Autowired private ComprarIngressoUseCase comprarIngresso;
    @Autowired private ConfirmarPagamentoUseCase confirmarPagamento;
    @Autowired private EventoRepositoryPort eventoRepository;
    @Autowired private LoteRepositoryPort loteRepository;
    @Autowired private IngressoRepositoryPort ingressoRepository;
    @Autowired private PedidoRepositoryPort pedidoRepository;
    @Autowired private PagamentoRepositoryPort pagamentoRepository;

    @Test
    void compraReservaOIngressoEOWebhookAprovadoConfirmaAVendaDeFormaIdempotente() {
        var evento = eventoRepository.salvar(
                Evento.novo("Congresso de Louvor", null, LocalDateTime.now().plusDays(30)));
        var lote = loteRepository.salvar(
                Lote.novo(evento.getId(), "Inteira", new BigDecimal("50.00"), 2));
        ingressoRepository.salvarTodos(List.of(
                Ingresso.disponivel(evento.getId(), lote.getId()),
                Ingresso.disponivel(evento.getId(), lote.getId())));

        var compra = comprarIngresso.comprar(new ComprarIngressoCommand(
                lote.getId(), "Maria", "maria@exemplo.com", "11999990000", MetodoPagamento.PIX));

        assertThat(compra.statusPedido()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(compra.valorTotal()).isEqualByComparingTo("50.00");
        assertThat(compra.qrCodePix()).startsWith("00020126FAKE-PIX-");
        assertThat(loteRepository.buscarPorId(lote.getId()).orElseThrow().getQuantidadeDisponivel())
                .isEqualTo(1);

        var pagamento = pagamentoRepository.buscarPorPedido(compra.pedidoId()).orElseThrow();
        assertThat(pagamento.getReferenciaGateway()).isNotBlank();

        var notificacao = new NotificacaoPagamento(pagamento.getReferenciaGateway(), ResultadoPagamento.APROVADO);
        confirmarPagamento.confirmar(notificacao);

        assertThat(pedidoRepository.buscarPorId(compra.pedidoId()).orElseThrow().getStatus())
                .isEqualTo(StatusPedido.PAGO);
        assertThat(pagamentoRepository.buscarPorPedido(compra.pedidoId()).orElseThrow().getStatus())
                .isEqualTo(StatusPagamento.APROVADO);
        var ingresso = ingressoRepository.listarPorPedido(compra.pedidoId()).get(0);
        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.VENDIDO);
        assertThat(ingresso.getCodigoQr()).isNotBlank();

        // Reentrega do webhook: idempotente, sem efeito nem exceção.
        confirmarPagamento.confirmar(notificacao);
        assertThat(pedidoRepository.buscarPorId(compra.pedidoId()).orElseThrow().getStatus())
                .isEqualTo(StatusPedido.PAGO);
    }
}
