package br.com.icb.ingressos.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.icb.ingressos.domain.Evento;
import br.com.icb.ingressos.domain.Ingresso;
import br.com.icb.ingressos.domain.Lote;
import br.com.icb.ingressos.domain.Pedido;
import br.com.icb.ingressos.domain.Usuario;
import br.com.icb.ingressos.domain.enums.StatusIngresso;
import br.com.icb.ingressos.domain.enums.StatusPedido;
import br.com.icb.ingressos.domain.exception.RecursoNaoEncontradoException;
import br.com.icb.ingressos.ports.out.EventoRepositoryPort;
import br.com.icb.ingressos.ports.out.IngressoRepositoryPort;
import br.com.icb.ingressos.ports.out.LoteRepositoryPort;
import br.com.icb.ingressos.ports.out.PedidoRepositoryPort;
import br.com.icb.ingressos.ports.out.UsuarioRepositoryPort;

@ExtendWith(MockitoExtension.class)
class ConsultarInscritosServiceTest {

    private static final Long EVENTO_ID = 1L;
    private static final LocalDateTime COMPRA = LocalDateTime.of(2026, 8, 30, 12, 0);

    @Mock private EventoRepositoryPort eventoRepository;
    @Mock private IngressoRepositoryPort ingressoRepository;
    @Mock private PedidoRepositoryPort pedidoRepository;
    @Mock private UsuarioRepositoryPort usuarioRepository;
    @Mock private LoteRepositoryPort loteRepository;

    @InjectMocks private ConsultarInscritosService service;

    private static Ingresso ingressoVendido(Long id, Long pedidoId) {
        return Ingresso.reconstituir(id, EVENTO_ID, 10L, pedidoId, "qr-" + id, StatusIngresso.VENDIDO);
    }

    @Test
    void montaOInscritoAPartirDoPedidoPagoDoClienteEDoLote() {
        when(eventoRepository.buscarPorId(EVENTO_ID)).thenReturn(Optional.of(
                Evento.reconstituir(EVENTO_ID, "Congresso", null, COMPRA.plusDays(30))));
        when(ingressoRepository.listarVendidosDoEvento(EVENTO_ID)).thenReturn(List.of(ingressoVendido(5L, 30L)));
        when(pedidoRepository.buscarPorId(30L)).thenReturn(Optional.of(
                Pedido.reconstituir(30L, 7L, COMPRA, new BigDecimal("50.00"), StatusPedido.PAGO)));
        when(usuarioRepository.buscarPorId(7L)).thenReturn(Optional.of(
                Usuario.reconstituir(7L, "Maria", "maria@exemplo.com", "11999990000", COMPRA)));
        when(loteRepository.buscarPorId(10L)).thenReturn(Optional.of(
                Lote.reconstituir(10L, EVENTO_ID, "Inteira", new BigDecimal("50.00"), 100, 99)));

        var inscritos = service.listarPorEvento(EVENTO_ID);

        assertThat(inscritos).hasSize(1);
        var inscrito = inscritos.get(0);
        assertThat(inscrito.nomeCliente()).isEqualTo("Maria");
        assertThat(inscrito.emailCliente()).isEqualTo("maria@exemplo.com");
        assertThat(inscrito.nomeLote()).isEqualTo("Inteira");
        assertThat(inscrito.codigoQr()).isEqualTo("qr-5");
        assertThat(inscrito.dataCompra()).isEqualTo(COMPRA);
    }

    @Test
    void ignoraIngressoCujoPedidoNaoEstaPago() {
        when(eventoRepository.buscarPorId(EVENTO_ID)).thenReturn(Optional.of(
                Evento.reconstituir(EVENTO_ID, "Congresso", null, COMPRA.plusDays(30))));
        when(ingressoRepository.listarVendidosDoEvento(EVENTO_ID)).thenReturn(List.of(ingressoVendido(5L, 30L)));
        when(pedidoRepository.buscarPorId(30L)).thenReturn(Optional.of(
                Pedido.reconstituir(30L, 7L, COMPRA, new BigDecimal("50.00"), StatusPedido.CANCELADO)));

        assertThat(service.listarPorEvento(EVENTO_ID)).isEmpty();
    }

    @Test
    void eventoInexistenteLancaRecursoNaoEncontrado() {
        when(eventoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.listarPorEvento(99L));
    }
}
