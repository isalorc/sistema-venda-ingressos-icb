package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ExpirarReservasServiceTest {

    private static final Long PEDIDO_ID = 10L;
    private static final Long EVENTO_ID = 1L;
    private static final Long LOTE_ID = 2L;
    private static final BigDecimal PRECO = new BigDecimal("50.00");

    @Mock private PedidoRepositoryPort pedidoRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private PagamentoRepositoryPort pagamentoRepository;

    private ExpirarReservasService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ExpirarReservasService(pedidoRepository, ingressoRepository, loteRepository,
                pagamentoRepository, clock, Duration.ofMinutes(15));
    }

    private static Pedido pedidoPendenteCriadoEm(LocalDateTime data) {
        return Pedido.reconstituir(PEDIDO_ID, 7L, data, PRECO, StatusPedido.PENDENTE);
    }

    @Test
    void expiraPedidoVencidoLiberaIngressoRepoeEstoqueECancelaPagamento() {
        var pedido = pedidoPendenteCriadoEm(LocalDateTime.of(2026, 8, 30, 11, 40));
        var ingresso = Ingresso.reconstituir(5L, EVENTO_ID, LOTE_ID, PEDIDO_ID, null, StatusIngresso.RESERVADO);
        var lote = Lote.reconstituir(LOTE_ID, EVENTO_ID, "Inteira", PRECO, 100, 99);
        var pagamento = Pagamento.reconstituir(3L, PEDIDO_ID, null, PRECO, MetodoPagamento.PIX,
                StatusPagamento.PENDENTE, "mp-cobranca-123");
        when(pedidoRepository.listarPendentesCriadosAntesDe(any())).thenReturn(List.of(pedido));
        when(ingressoRepository.listarPorPedido(PEDIDO_ID)).thenReturn(List.of(ingresso));
        when(loteRepository.buscarPorId(LOTE_ID)).thenReturn(Optional.of(lote));
        when(pagamentoRepository.buscarPorPedido(PEDIDO_ID)).thenReturn(Optional.of(pagamento));

        var expirados = service.expirarReservasVencidas();

        assertThat(expirados).isEqualTo(1);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EXPIRADO);
        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.DISPONIVEL);
        assertThat(lote.getQuantidadeDisponivel()).isEqualTo(100);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
    }

    @Test
    void consultaOsPendentesUsandoAgoraMenosOTtlComoLimite() {
        when(pedidoRepository.listarPendentesCriadosAntesDe(LocalDateTime.of(2026, 8, 30, 11, 45)))
                .thenReturn(List.of());

        var expirados = service.expirarReservasVencidas();

        assertThat(expirados).isZero();
        verify(pedidoRepository).listarPendentesCriadosAntesDe(LocalDateTime.of(2026, 8, 30, 11, 45));
    }

    @Test
    void semPedidosVencidosNaoTocaEmIngressosLotesOuPagamentos() {
        when(pedidoRepository.listarPendentesCriadosAntesDe(any())).thenReturn(List.of());

        service.expirarReservasVencidas();

        verifyNoInteractions(ingressoRepository, loteRepository, pagamentoRepository);
    }
}
