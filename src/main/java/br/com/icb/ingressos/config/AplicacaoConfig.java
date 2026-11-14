package br.com.icb.ingressos.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de infraestrutura transversal aos casos de uso.
 *
 * <p>O relógio é exposto como {@link Clock} do próprio JDK — sem porta dedicada
 * (decisão do Épico 2): os casos de uso recebem o {@code Clock} e derivam a hora
 * com {@code LocalDateTime.now(clock)}, o que mantém o núcleo testável (basta um
 * {@link Clock#fixed}) sem introduzir abstração que não se paga.
 *
 * <p>As datas da API são horário local sem fuso (ex.: início do evento, abertura
 * do lote, prazo da reserva), no fuso da igreja. Por isso o relógio usa um fuso
 * fixo ({@code app.fuso-horario}) e não o da máquina: um servidor em UTC
 * deslocaria tudo em 3 horas.
 */
@Configuration
public class AplicacaoConfig {

    @Bean
    public Clock clock(@Value("${app.fuso-horario}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
