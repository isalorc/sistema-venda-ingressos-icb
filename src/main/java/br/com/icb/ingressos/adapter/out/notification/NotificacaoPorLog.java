package br.com.icb.ingressos.adapter.out.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.icb.ingressos.ports.out.NotificacaoPort;

/**
 * Implementação <em>stub</em> da notificação ao cliente (RF-15).
 *
 * <p>Enquanto o template do e-mail do ingresso não é definido (pendência em
 * {@code docs/regrasDeNegocio.md}), apenas registra em log que o ingresso seria
 * enviado. O envio real (SMTP/serviço de e-mail) entra em épico posterior.
 */
@Component
public class NotificacaoPorLog implements NotificacaoPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoPorLog.class);

    @Override
    public void enviarIngresso(IngressoEmitido ingresso) {
        log.info("[notificacao-stub] ingresso de \"{}\" ({}) para {} — evento \"{}\" em {}, QR {}",
                ingresso.nomeCliente(), ingresso.emailCliente(), ingresso.emailCliente(),
                ingresso.nomeEvento(), ingresso.dataHoraEvento(), ingresso.codigoQr());
    }
}
