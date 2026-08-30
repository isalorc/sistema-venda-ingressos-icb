package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.in.ComprarIngressoUseCase.ComprarIngressoCommand;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort.CobrancaCriada;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PagamentoRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ComprarIngressoServiceTest {

    private static final Long LOTE_ID = 2L;
    private static final Long EVENTO_ID = 1L;
    private static final BigDecimal PRECO = new BigDecimal("50.00");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 30, 12, 0);

    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private LoteRepositoryPort loteRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;
    @Mock private PedidoRepositoryPort pedidoRepository;
    @Mock private PagamentoRepositoryPort pagamentoRepository;
    @Mock private GatewayPagamentoPort gatewayPagamento;

    private ComprarIngressoService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);
        service = new ComprarIngressoService(usuarioRepository, loteRepository, ingressoRepository,
                pedidoRepository, pagamentoRepository, gatewayPagamento, clock);
    }

    private static ComprarIngressoCommand comando() {
        return new ComprarIngressoCommand(LOTE_ID, "Maria", "MARIA@Email.com", "11999990000", MetodoPagamento.PIX);
    }

    private static Lote loteComEstoque(int disponivel) {
        return Lote.reconstituir(LOTE_ID, EVENTO_ID, "Inteira", PRECO, 100, disponivel);
    }

    private static Ingresso ingressoDisponivel() {
        return Ingresso.reconstituir(5L, EVENTO_ID, LOTE_ID, null, null, StatusIngresso.DISPONIVEL);
    }

    private void stubFluxoFeliz() {
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(loteComEstoque(100)));
        when(ingressoRepository.buscarPrimeiroDisponivelDoLote(LOTE_ID)).thenReturn(Optional.of(ingressoDisponivel()));
        when(pedidoRepository.salvar(any())).thenReturn(
                Pedido.reconstituir(10L, 7L, AGORA, PRECO, StatusPedido.PENDENTE));
        when(ingressoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pagamentoRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gatewayPagamento.criarCobranca(any())).thenReturn(
                new CobrancaCriada("mp-cobranca-123", "https://pay/checkout", "pix-payload"));
    }

    @Test
    void compraComSucessoReservaIngressoCriaPedidoERetornaDadosDePagamento() {
        stubFluxoFeliz();
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(Optional.empty());
        when(usuarioRepository.salvar(any())).thenReturn(
                Usuario.reconstituir(7L, "Maria", "maria@email.com", "11999990000", AGORA));

        var resultado = service.comprar(comando());

        assertThat(resultado.pedidoId()).isEqualTo(10L);
        assertThat(resultado.ingressoId()).isEqualTo(5L);
        assertThat(resultado.valorTotal()).isEqualByComparingTo(PRECO);
        assertThat(resultado.statusPedido()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(resultado.urlPagamento()).isEqualTo("https://pay/checkout");
        assertThat(resultado.qrCodePix()).isEqualTo("pix-payload");
    }

    @Test
    void reservaDecrementaOEstoqueDoLoteEMarcaOIngresso() {
        stubFluxoFeliz();
        when(usuarioRepository.buscarPorEmail(any())).thenReturn(
                Optional.of(Usuario.reconstituir(7L, "Maria", "maria@email.com", "11999990000", AGORA)));

        service.comprar(comando());

        var loteSalvo = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).salvar(loteSalvo.capture());
        assertThat(loteSalvo.getValue().getQuantidadeDisponivel()).isEqualTo(99);

        var ingressoSalvo = ArgumentCaptor.forClass(Ingresso.class);
        verify(ingressoRepository).salvar(ingressoSalvo.capture());
        assertThat(ingressoSalvo.getValue().getStatus()).isEqualTo(StatusIngresso.RESERVADO);
        assertThat(ingressoSalvo.getValue().getPedidoId()).isEqualTo(10L);
    }

    @Test
    void vinculaAReferenciaDaCobrancaAoPagamentoPendente() {
        stubFluxoFeliz();
        when(usuarioRepository.buscarPorEmail(any())).thenReturn(
                Optional.of(Usuario.reconstituir(7L, "Maria", "maria@email.com", "11999990000", AGORA)));

        service.comprar(comando());

        var pagamentoSalvo = ArgumentCaptor.forClass(Pagamento.class);
        verify(pagamentoRepository, org.mockito.Mockito.times(2)).salvar(pagamentoSalvo.capture());
        var ultimo = pagamentoSalvo.getValue();
        assertThat(ultimo.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(ultimo.getReferenciaGateway()).isEqualTo("mp-cobranca-123");
    }

    @Test
    void clienteExistenteEhReaproveitadoPeloEmailNormalizado() {
        stubFluxoFeliz();
        when(usuarioRepository.buscarPorEmail("maria@email.com")).thenReturn(
                Optional.of(Usuario.reconstituir(7L, "Maria", "maria@email.com", "11999990000", AGORA)));

        service.comprar(comando());

        verify(usuarioRepository, never()).salvar(any());
    }

    @Test
    void loteInexistenteLancaRecursoNaoEncontradoSemCriarPedido() {
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.comprar(comando()));

        verify(pedidoRepository, never()).salvar(any());
        verify(gatewayPagamento, never()).criarCobranca(any());
    }

    @Test
    void loteSemIngressoDisponivelLancaIngressoEsgotadoSemCriarPedido() {
        when(loteRepository.buscarPorIdComBloqueio(LOTE_ID)).thenReturn(Optional.of(loteComEstoque(100)));
        when(ingressoRepository.buscarPrimeiroDisponivelDoLote(LOTE_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(IngressoEsgotadoException.class)
                .isThrownBy(() -> service.comprar(comando()));

        verify(pedidoRepository, never()).salvar(any());
        verify(gatewayPagamento, never()).criarCobranca(any());
    }
}
