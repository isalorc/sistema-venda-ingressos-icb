package br.com.icb.ingressos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita o agendamento de tarefas ({@code @Scheduled}) — hoje só a varredura de
 * expiração de reservas (RF-17). Isolado do {@link AplicacaoConfig} para manter
 * cada configuração com uma responsabilidade.
 *
 * <p>MVP roda em instância única (RNF-24); múltiplas instâncias exigirão trava
 * distribuída (ex.: ShedLock), previsto para depois.
 */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
