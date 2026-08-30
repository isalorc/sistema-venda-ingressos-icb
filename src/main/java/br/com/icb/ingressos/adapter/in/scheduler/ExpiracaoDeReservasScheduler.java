package br.com.icb.ingressos.adapter.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.icb.ingressos.ports.in.ExpirarReservasUseCase;

/**
 * Adapter de entrada que dispara periodicamente a expiração de reservas não pagas
 * (RF-17 / RN-2). O intervalo entre varreduras é configurável em
 * {@code app.reserva.intervalo-varredura}.
 *
 * <p>Uma falha numa varredura é logada e engolida (RNF-28): não pode interromper o
 * agendamento — a próxima execução tenta de novo.
 */
@Component
public class ExpiracaoDeReservasScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoDeReservasScheduler.class);

    private final ExpirarReservasUseCase expirarReservas;

    public ExpiracaoDeReservasScheduler(ExpirarReservasUseCase expirarReservas) {
        this.expirarReservas = expirarReservas;
    }

    @Scheduled(fixedDelayString = "${app.reserva.intervalo-varredura}")
    public void varrerReservasVencidas() {
        try {
            expirarReservas.expirarReservasVencidas();
        } catch (RuntimeException excecao) {
            log.warn("Falha na varredura de expiração de reservas; próxima execução tentará de novo.", excecao);
        }
    }
}
