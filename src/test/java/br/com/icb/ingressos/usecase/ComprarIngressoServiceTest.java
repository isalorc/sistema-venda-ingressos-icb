package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
import br.com.icb.ingressos.domain.exception.IngressoEsgotadoException;
import br.com.icb.ingressos.domain.exception.LoteIndisponivelException;
import br.com.icb.ingressos.domain.exception.QuantidadeDeIngressosInvalidaException;
import br.com.icb.ingressos.domain.exception.QuantidadeIndisponivelException;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.domain.exception.TransicaoInvalidaException;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ComprarIngressoCommand;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort.CobrancaCriada;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort.DadosCobranca;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ComprarIngressoServiceTest {

    private static final Long CLIENTE_ID = 7L;
    private static final Long LOTE_ID = 2L;
    private static final Long EVENTO_ID = 1L;
    private static final BigDecimal PRECO = new BigDecimal("50.00");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);
    private static final int MAXIMO = 5;

    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;
    @Mock private PedidoRepositoryPort pedidoRepository;
    @Mock private PagamentoRepositoryPort pagamentoRepository;
    @Mock private GatewayPagamentoPort gatewayPagamento;

    private ComprarIngressoService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ComprarIngressoService(usuarioRepository, eventoRepository, loteRepository,
                ingressoRepository, pedidoRepository, pagamentoRepository, gatewayPagamento, clock, MAXIMO);
    }

    private static ComprarIngressoCommand comando(String... participantes) {
        return new ComprarIngressoCommand(CLIENTE_ID, LOTE_ID, List.of(participantes), MetodoPagamento.PIX);
    }

    private static Usuario clienteComConta() {
        return Usuario.reconstituir(CLIENTE_ID, "Maria", "maria@email.com", "11999990000", "hash", AGORA);
    }

    private static Lote loteComEstoque(int disponivel) {
        return Lote.reconstituir(LOTE_ID, EVENTO_ID, "Inteira", PRECO, 100, disponivel);
    }

    private static List<Ingresso> ingressosDisponiveis(int quantidade) {
        var ingressos = new ArrayList<Ingresso>();
        for (var i = 0; i < quantidade; i++) {
            ingressos.add(Ingresso.reconstituir(5L + i, EVENTO_ID, LOTE_ID, null, null,
                    StatusIngresso.DISPONIVEL));
        }
        return ingressos;
    }

    private void stubCliente() {
        when(usuarioRepository.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(clienteComConta()));
    }

    private void stubEventoAtivo() {
        when(eventoRepository.buscarPorId(EVENTO_ID)).thenReturn(Optional.of(
                Evento.reconstituir(EVENTO_ID, "Congresso", null, AGORA.plusDays(30))));
    }

    private void stubFluxoFeliz(int quantidade) {
        stubCliente();
        stubEventoAtivo();
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(loteComEstoque(100)));
        when(ingressoRepository.listarDisponiveisDoLote(LOTE_ID, quantidade))
                .thenReturn(ingressosDisponiveis(quantidade));
        when(pedidoRepository.salvar(any())).thenAnswer(inv -> {
            Pedido novo = inv.getArgument(0);
            return Pedido.reconstituir(10L, novo.getUsuarioId(), novo.getLoteId(),
                    novo.getQuantidadeIngressos(), novo.getDataPedido(), novo.getValorTotal(), novo.getStatus());
        });
        when(ingressoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagamentoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gatewayPagamento.criarCobranca(any())).thenReturn(
                new CobrancaCriada("mp-cobranca-123", "https://pay/checkout", "pix-payload"));
    }

    @Test
    void compraDeUmIngressoReservaCriaPedidoERetornaDadosDePagamento() {
        stubFluxoFeliz(1);

        var resultado = service.comprar(comando("Maria"));

        assertThat(resultado.pedidoId()).isEqualTo(10L);
        assertThat(resultado.ingressoIds()).containsExactly(5L);
        assertThat(resultado.valorTotal()).isEqualByComparingTo(PRECO);
        assertThat(resultado.statusPedido()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resultado.urlPagamento()).isEqualTo("https://pay/checkout");
        assertThat(resultado.qrCodePix()).isEqualTo("pix-payload");
    }

    @Test
    void compraDeVariosIngressosReservaUmPorParticipanteECobraOTotal() {
        stubFluxoFeliz(3);

        var resultado = service.comprar(comando("Maria", " João ", "Ana"));

        assertThat(resultado.ingressoIds()).containsExactly(5L, 6L, 7L);
        assertThat(resultado.valorTotal()).isEqualByComparingTo("150.00");

        var ingressosSalvos = ArgumentCaptor.forClass(Ingresso.class);
        verify(ingressoRepository, times(3)).salvar(ingressosSalvos.capture());
        assertThat(ingressosSalvos.getAllValues())
                .extracting(Ingresso::getNomeParticipante)
                .containsExactly("Maria", "João", "Ana");
        assertThat(ingressosSalvos.getAllValues())
                .allSatisfy(ingresso -> {
                    assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.RESERVADO);
                    assertThat(ingresso.getPedidoId()).isEqualTo(10L);
                });

        var pedidoSalvo = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).salvar(pedidoSalvo.capture());
        assertThat(pedidoSalvo.getValue().getQuantidadeIngressos()).isEqualTo(3);
        assertThat(pedidoSalvo.getValue().getLoteId()).isEqualTo(LOTE_ID);
        assertThat(pedidoSalvo.getValue().getUsuarioId()).isEqualTo(CLIENTE_ID);

        var cobranca = ArgumentCaptor.forClass(DadosCobranca.class);
        verify(gatewayPagamento).criarCobranca(cobranca.capture());
        assertThat(cobranca.getValue().valor()).isEqualByComparingTo("150.00");
    }

    @Test
    void reservaDecrementaOEstoqueDoLotePelaQuantidade() {
        stubFluxoFeliz(2);

        service.comprar(comando("Maria", "João"));

        var loteSalvo = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).salvar(loteSalvo.capture());
        assertThat(loteSalvo.getValue().getQuantidadeDisponivel()).isEqualTo(98);
    }

    @Test
    void vinculaAReferenciaDaCobrancaAoPagamentoPendente() {
        stubFluxoFeliz(1);

        service.comprar(comando("Maria"));

        var pagamentoSalvo = ArgumentCaptor.forClass(Pagamento.class);
        verify(pagamentoRepository, times(2)).salvar(pagamentoSalvo.capture());
        var ultimo = pagamentoSalvo.getValue();
        assertThat(ultimo.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(ultimo.getReferenciaGateway()).isEqualTo("mp-cobranca-123");
    }

    @Test
    void semParticipantesRecusaAntesDeTocarNoEstoque() {
        assertThatExceptionOfType(QuantidadeDeIngressosInvalidaException.class)
                .isThrownBy(() -> service.comprar(comando()))
                .withMessageContaining("1 a 5");

        verifyNoInteractions(loteRepository, pedidoRepository, gatewayPagamento);
    }

    @Test
    void acimaDoLimitePorPedidoRecusa() {
        assertThatExceptionOfType(QuantidadeDeIngressosInvalidaException.class)
                .isThrownBy(() -> service.comprar(comando("A", "B", "C", "D", "E", "F")));

        verifyNoInteractions(loteRepository, pedidoRepository);
    }

    @Test
    void participanteSemNomeRecusa() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.comprar(comando("Maria", "  ")))
                .withMessageContaining("nome de cada participante");
    }

    @Test
    void usuarioSemContaNaoCompra() {
        when(usuarioRepository.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(
                Usuario.reconstituir(CLIENTE_ID, "Maria", "maria@email.com", "11999990000", AGORA)));

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")));

        verifyNoInteractions(loteRepository);
    }

    @Test
    void loteInexistenteLancaRecursoNaoEncontradoSemCriarPedido() {
        stubCliente();
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")));

        verify(pedidoRepository, never()).salvar(any());
        verify(gatewayPagamento, never()).criarCobranca(any());
    }

    @Test
    void estoqueMenorQueAQuantidadeRecusaSemReservarNada() {
        stubCliente();
        stubEventoAtivo();
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(loteComEstoque(2)));

        assertThatExceptionOfType(QuantidadeIndisponivelException.class)
                .isThrownBy(() -> service.comprar(comando("A", "B", "C")))
                .withMessage("Restam apenas 2 ingressos neste lote.");

        verify(pedidoRepository, never()).salvar(any());
        verify(ingressoRepository, never()).salvar(any());
    }

    @Test
    void loteSemIngressoDisponivelLancaIngressoEsgotadoSemCriarPedido() {
        stubCliente();
        stubEventoAtivo();
        var esgotado = loteComEstoque(0);
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(esgotado));
        when(loteRepository.listarPorEvento(EVENTO_ID)).thenReturn(List.of(esgotado));

        assertThatExceptionOfType(IngressoEsgotadoException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")));

        verify(pedidoRepository, never()).salvar(any());
        verify(gatewayPagamento, never()).criarCobranca(any());
    }

    @Test
    void eventoCanceladoRecusaACompra() {
        stubCliente();
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(loteComEstoque(100)));
        when(eventoRepository.buscarPorId(EVENTO_ID)).thenReturn(Optional.of(Evento.reconstituir(
                EVENTO_ID, "Cancelado", null, AGORA.plusDays(30), null, null, AGORA.minusDays(1))));

        assertThatExceptionOfType(TransicaoInvalidaException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")));

        verify(pedidoRepository, never()).salvar(any());
    }

    @Test
    void loteAgendadoRecusaACompraSemCriarPedido() {
        stubCliente();
        stubEventoAtivo();
        var agendado = Lote.reconstituir(LOTE_ID, EVENTO_ID, "2º Lote", PRECO, 100, 100,
                AGORA.plusDays(10), null);
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(agendado));
        when(loteRepository.listarPorEvento(EVENTO_ID)).thenReturn(List.of(agendado));

        assertThatExceptionOfType(LoteIndisponivelException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")))
                .withMessageContaining("ainda não começaram");

        verify(pedidoRepository, never()).salvar(any());
    }

    @Test
    void loteEncerradoRecusaACompra() {
        stubCliente();
        stubEventoAtivo();
        var encerrado = Lote.reconstituir(LOTE_ID, EVENTO_ID, "1º Lote", PRECO, 100, 40,
                null, AGORA.minusDays(1));
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(encerrado));
        when(loteRepository.listarPorEvento(EVENTO_ID)).thenReturn(List.of(encerrado));

        assertThatExceptionOfType(LoteIndisponivelException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")))
                .withMessageContaining("encerradas");
    }

    @Test
    void loteNaFilaRecusaACompraEnquantoOAnteriorEstaAtivo() {
        stubCliente();
        stubEventoAtivo();
        var loteAnterior = Lote.reconstituir(1L, EVENTO_ID, "1º Lote", PRECO, 100, 10);
        var loteNaFila = loteComEstoque(100);
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(loteNaFila));
        when(loteRepository.listarPorEvento(EVENTO_ID)).thenReturn(List.of(loteAnterior, loteNaFila));

        assertThatExceptionOfType(LoteIndisponivelException.class)
                .isThrownBy(() -> service.comprar(comando("Maria")))
                .withMessageContaining("ainda não está disponível");
    }
}
