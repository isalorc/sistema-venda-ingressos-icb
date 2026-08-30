package br.com.icb.ingressos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/**
 * Metadados do contrato OpenAPI publicado pelo springdoc (RNF-36). O detalhamento
 * de cada operação fica nas anotações dos controllers; aqui só a identificação da
 * API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contratoDaApi(@Value("${spring.application.name}") String nome) {
        return new OpenAPI().info(new Info()
                .title("API de Venda de Ingressos — ICB")
                .version("1.0.0-SNAPSHOT")
                .description("""
                        API REST de venda de ingressos para os eventos da Igreja ICB. \
                        Cliente consulta eventos e compra ingressos (sem cadastro); \
                        o administrador cadastra eventos e lotes e acompanha os inscritos; \
                        o gateway de pagamento confirma o resultado pelo webhook. \
                        Projeto de TCC (Fatec) — arquitetura hexagonal.""")
                .contact(new Contact().name("TCC — " + nome)));
    }
}
