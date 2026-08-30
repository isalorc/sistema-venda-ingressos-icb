package br.com.icb.ingressos.adapter.out.payment;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.icb.ingressos.domain.enums.MetodoPagamento;
import br.com.icb.ingressos.ports.out.GatewayPagamentoPort;

/**
 * Implementação <em>fake</em> do gateway de pagamento para o MVP (Épico 6.1).
 *
 * <p>Não conversa com nenhum provedor: gera uma referência aleatória e devolve
 * dados de pagamento sintéticos. A confirmação do resultado é simulada chamando
 * o webhook ({@code POST /api/webhooks/pagamento}) manualmente. A integração real
 * com o Mercado Pago (sandbox) entra em épico posterior e substitui este bean.
 */
@Component
public class GatewayPagamentoFake implements GatewayPagamentoPort {

    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoFake.class);

    @Override
    public CobrancaCriada criarCobranca(DadosCobranca dados) {
        var referencia = "fake-" + UUID.randomUUID();
        log.info("[gateway-fake] cobrança {} criada para o pedido {} ({} — R$ {})",
                referencia, dados.pedidoId(), dados.metodo(), dados.valor());

        var urlPagamento = "https://pagamento.fake/checkout/" + referencia;
        var qrCodePix = dados.metodo() == MetodoPagamento.PIX
                ? "00020126FAKE-PIX-" + referencia
                : null;
        return new CobrancaCriada(referencia, urlPagamento, qrCodePix);
    }
}
