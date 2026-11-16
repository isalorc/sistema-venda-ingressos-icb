package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConsultarPedidosClienteServiceTest {

    private static final Long CLIENTE_ID = 7L;
    private static final LocalDateTime DATA_PEDIDO = LocalDateTime.of(2026, 8, 30, 12, 0);

    @Mock private PedidoRepositoryPort pedidoRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private PagamentoRepositoryPort pagamentoRepository;

    private ConsultarPedidosClienteService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarPedidosClienteService(pedidoRepository, ingressoRepository, loteRepository,
                eventoRepository, pagamentoRepository, Duration.ofMinutes(15));
    }

    private static Pedido pedido(Long id, StatusPedido status) {
        return Pedido.reconstituir(id, CLIENTE_ID, 2L, 2, DATA_PEDIDO, new BigDecimal("100.00"), status);
    }

    private void stubEvento() {
        when(loteRepository.buscarPorId(2L)).thenReturn(Optional.of(
                Lote.reconstituir(2L, 1L, "1º Lote", new BigDecimal("50.00"), 100, 98)));
        when(eventoRepository.buscarPorId(1L)).thenReturn(Optional.of(
                Evento.reconstituir(1L, "Congresso", null, DATA_PEDIDO.plusDays(30))));
    }

    @Test
    void listaOsPedidosComEventoEPrazoSoNoPendente() {
        when(pedidoRepository.listarPorUsuario(CLIENTE_ID)).thenReturn(List.of(
                pedido(11L, StatusPedido.PENDENTE), pedido(10L, StatusPedido.PAGO)));
        stubEvento();

        var pedidos = service.listar(CLIENTE_ID);

        assertThat(pedidos).hasSize(2);
        assertThat(pedidos.get(0).expiraEm()).isEqualTo(DATA_PEDIDO.plusMinutes(15));
        assertThat(pedidos.get(1).expiraEm()).isNull();
        assertThat(pedidos.get(0).quantidadeIngressos()).isEqualTo(2);
        assertThat(pedidos.get(0).evento().nome()).isEqualTo("Congresso");
        assertThat(pedidos.get(0).evento().nomeLote()).isEqualTo("1º Lote");
    }

    @Test
    void pedidoAntigoSemLoteVemSemEvento() {
        when(pedidoRepository.listarPorUsuario(CLIENTE_ID)).thenReturn(List.of(
                Pedido.reconstituir(5L, CLIENTE_ID, DATA_PEDIDO, new BigDecimal("50.00"), StatusPedido.EXPIRADO)));

        assertThat(service.listar(CLIENTE_ID).get(0).evento()).isNull();
    }

    @Test
    void detalheTrazPagamentoEIngressosComQr() {
        when(pedidoRepository.buscarPorId(10L)).thenReturn(Optional.of(pedido(10L, StatusPedido.PAGO)));
        stubEvento();
        when(pagamentoRepository.buscarPorPedido(10L)).thenReturn(Optional.of(
                Pagamento.pendente(10L, new BigDecimal("100.00"), MetodoPagamento.PIX)));
        when(ingressoRepository.listarPorPedido(10L)).thenReturn(List.of(
                Ingresso.reconstituir(6L, 1L, 2L, 10L, "João", "qr-6", StatusIngresso.VENDIDO),
                Ingresso.reconstituir(5L, 1L, 2L, 10L, "Maria", "qr-5", StatusIngresso.VENDIDO)));

        var detalhe = service.detalhar(CLIENTE_ID, 10L);

        assertThat(detalhe.metodoPagamento()).isEqualTo(MetodoPagamento.PIX);
        assertThat(detalhe.ingressos()).extracting(i -> i.nomeParticipante()).containsExactly("Maria", "João");
        assertThat(detalhe.ingressos()).extracting(i -> i.codigoQr()).containsExactly("qr-5", "qr-6");
    }

    @Test
    void pedidoDeOutroClienteRespondeComoInexistente() {
        when(pedidoRepository.buscarPorId(10L)).thenReturn(Optional.of(
                Pedido.reconstituir(10L, 99L, 2L, 1, DATA_PEDIDO, new BigDecimal("50.00"), StatusPedido.PAGO)));

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.detalhar(CLIENTE_ID, 10L));
    }
}
