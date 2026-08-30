package br.com.icb.ingressos.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de infraestrutura transversal aos casos de uso.
 *
 * <p>O relógio é exposto como {@link Clock} do próprio JDK — sem porta dedicada
 * (decisão do Épico 2): os casos de uso recebem o {@code Clock} e derivam a hora
 * com {@code LocalDateTime.now(clock)}, o que mantém o núcleo testável (basta um
 * {@link Clock#fixed}) sem introduzir abstração que não se paga.
 */
@Configuration
public class AplicacaoConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
