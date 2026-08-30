package br.com.icb.ingressos.adapter.out.persistence.jpa;

import java.io.IOException;
import java.io.UncheckedIOException;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * PostgreSQL real e efêmero para os testes de integração da persistência, sem
 * Docker. O binário nativo é baixado pelo Zonky e sobe uma única vez por
 * execução da suíte (instância compartilhada entre as classes de teste); o
 * processo é encerrado no shutdown da JVM.
 */
final class PostgresDeTeste {

    static final EmbeddedPostgres INSTANCIA = iniciar();

    private PostgresDeTeste() {
    }

    private static EmbeddedPostgres iniciar() {
        try {
            var postgres = EmbeddedPostgres.builder().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> fechar(postgres)));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao subir o PostgreSQL embarcado (Zonky).", e);
        }
    }

    private static void fechar(EmbeddedPostgres postgres) {
        try {
            postgres.close();
        } catch (IOException ignorado) {
            // encerramento de melhor esforço no shutdown da JVM
        }
    }

    /** URL JDBC do banco padrão (\"postgres\") com o usuário \"postgres\". */
    static String jdbcUrl() {
        return INSTANCIA.getJdbcUrl("postgres", "postgres");
    }
}
