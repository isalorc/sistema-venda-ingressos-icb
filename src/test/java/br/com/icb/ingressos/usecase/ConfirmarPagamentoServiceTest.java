package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.Pagamento;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPagamento;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.NotificacaoPagamento;
import br.com.icb.ingressos.ports.in.ConfirmarPagamentoUseCase.ResultadoPagamento;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.NotificacaoPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConfirmarPagamentoServiceTest {

    private static final String REFERENCIA = "mp-cobranca-123";
    private static final Long PEDIDO_ID = 10L;
    private static final Long USUARIO_ID = 7L;
    private static final Long EVENTO_ID = 1L;
    private static final Long LOTE_ID = 2L;
    private static final BigDecimal PRECO = new BigDecimal("50.00");
    private static final LocalDateTime CRIADO_EM = LocalDateTime.of(2026, 8, 30, 11, 0);

    @Mock private PagamentoRepositoryPort pagamentoRepository;
    @Mock private PedidoRepositoryPort pedidoRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private NotificacaoPort notificacao;

    private ConfirmarPagamentoService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:15:00Z"), ZoneOffset.UTC);
        service = new ConfirmarPagamentoService(pagamentoRepository, pedidoRepository, ingressoRepository,
                loteRepository, usuarioRepository, eventoRepository, notificacao, clock);
    }

    private static Pagamento pagamentoPendente() {
        return Pagamento.reconstituir(3L, PEDIDO_ID, null, PRECO, MetodoPagamento.PIX,
                StatusPagamento.PENDENTE, REFERENCIA);
    }

    private static Pedido pedidoPendente() {
        return Pedido.reconstituir(PEDIDO_ID, USUARIO_ID, CRIADO_EM, PRECO, StatusPedido.PENDENTE);
    }

    private static Ingresso ingressoReservado() {
        return Ingresso.reconstituir(5L, EVENTO_ID, LOTE_ID, PEDIDO_ID, null, StatusIngresso.RESERVADO);
    }

    private static NotificacaoPagamento notificacaoDe(ResultadoPagamento resultado) {
        return new NotificacaoPagamento(REFERENCIA, resultado);
    }

    @Test
    void aprovadoConfirmaVendaGeraQrMarcaPedidoPagoENotificaOCliente() {
        var pagamento = pagamentoPendente();
        var pedido = pedidoPendente();
        var ingresso = ingressoReservado();
        when(pagamentoRepository.buscarPorReferenciaGateway(REFERENCIA)).thenReturn(Optional.of(pagamento));
        when(pedidoRepository.buscarPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(ingressoRepository.listarPorPedido(PEDIDO_ID)).thenReturn(List.of(ingresso));
        when(usuarioRepository.buscarPorId(USUARIO_ID)).thenReturn(Optional.of(
                Usuario.reconstituir(USUARIO_ID, "Maria", "maria@email.com", "11999990000", CRIADO_EM)));
        when(eventoRepository.buscarPorId(EVENTO_ID)).thenReturn(Optional.of(
                Evento.reconstituir(EVENTO_ID, "Culto de Natal", null, LocalDateTime.of(2026, 12, 24, 19, 0))));

        service.confirmar(notificacaoDe(ResultadoPagamento.APROVADO));

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.getDataPagamento()).isEqualTo(LocalDateTime.of(2026, 8, 30, 12, 15));
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.VENDIDO);
        assertThat(ingresso.getCodigoQr()).isNotBlank();

        var emitido = ArgumentCaptor.forClass(NotificacaoPort.IngressoEmitido.class);
        verify(notificacao).enviarIngresso(emitido.capture());
        assertThat(emitido.getValue().emailCliente()).isEqualTo("maria@email.com");
        assertThat(emitido.getValue().nomeEvento()).isEqualTo("Culto de Natal");
        assertThat(emitido.getValue().codigoQr()).isEqualTo(ingresso.getCodigoQr());
    }

    @Test
    void recusadoLiberaAReservaRepoeEstoqueECancelaPagamentoEPedido() {
        var pagamento = pagamentoPendente();
        var pedido = pedidoPendente();
        var ingresso = ingressoReservado();
        var lote = Lote.reconstituir(LOTE_ID, EVENTO_ID, "Inteira", PRECO, 100, 99);
        when(pagamentoRepository.buscarPorReferenciaGateway(REFERENCIA)).thenReturn(Optional.of(pagamento));
        when(pedidoRepository.buscarPorId(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(ingressoRepository.listarPorPedido(PEDIDO_ID)).thenReturn(List.of(ingresso));
        when(loteRepository.buscarPorId(LOTE_ID)).thenReturn(Optional.of(lote));

        service.confirmar(notificacaoDe(ResultadoPagamento.RECUSADO));

        assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.DISPONIVEL);
        assertThat(lote.getQuantidadeDisponivel()).isEqualTo(100);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        verify(notificacao, never()).enviarIngresso(any());
    }

    @Test
    void notificacaoDuplicadaEhIgnoradaQuandoPagamentoJaSaiuDePendente() {
        var jaAprovado = Pagamento.reconstituir(3L, PEDIDO_ID, LocalDateTime.of(2026, 8, 30, 12, 10),
                PRECO, MetodoPagamento.PIX, StatusPagamento.APROVADO, REFERENCIA);
        when(pagamentoRepository.buscarPorReferenciaGateway(REFERENCIA)).thenReturn(Optional.of(jaAprovado));

        service.confirmar(notificacaoDe(ResultadoPagamento.APROVADO));

        verify(pedidoRepository, never()).buscarPorId(any());
        verify(pagamentoRepository, never()).salvar(any());
        verifyNoInteractions(ingressoRepository, notificacao);
    }

    @Test
    void referenciaDesconhecidaLancaRecursoNaoEncontrado() {
        when(pagamentoRepository.buscarPorReferenciaGateway(REFERENCIA)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.confirmar(notificacaoDe(ResultadoPagamento.APROVADO)));
    }
}
