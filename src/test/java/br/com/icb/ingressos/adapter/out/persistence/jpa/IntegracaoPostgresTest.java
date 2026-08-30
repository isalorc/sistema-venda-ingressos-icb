package br.com.icb.ingressos.adapter.out.persistence.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base dos testes de integração da persistência JPA sobre PostgreSQL real
 * (Zonky embarcado — ver {@link PostgresDeTeste}). O perfil {@code it} não herda
 * as exclusões de autoconfig do perfil {@code dev}, então DataSource, JPA e
 * Flyway sobem normalmente; o datasource aponta para o Postgres embarcado.
 */
@SpringBootTest
@ActiveProfiles("it")
abstract class IntegracaoPostgresTest {

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registro) {
        registro.add("app.persistencia", () -> "postgres");
        registro.add("spring.datasource.url", PostgresDeTeste::jdbcUrl);
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
        // Folga para o teste de concorrência (RNF-30) não esbarrar no pool.
        registro.add("spring.datasource.hikari.maximum-pool-size", () -> "25");
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registro.add("spring.jpa.open-in-view", () -> "false");
        registro.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparBanco() {
        jdbcTemplate.execute(
                "truncate table ingresso, pagamento, pedido, lote, evento, usuario restart identity cascade");
    }
}
