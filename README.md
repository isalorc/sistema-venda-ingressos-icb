# Sistema de venda de ingressos ICB

API para gerenciar a venda de ingressos dos eventos da Igreja (ICB).
TCC do curso de Análise e Desenvolvimento de Sistemas da Fatec.

Backend em Java com **Arquitetura Hexagonal (Ports & Adapters)**. Este
repositório entrega apenas a **API REST + webhook de pagamento**; o frontend fica
em projeto separado.

## Motivação

A igreja vende ingressos hoje pela Sympla, que cobra **taxa de serviço de 10%**
por ingresso (mínimo R$ 3,99 para ingressos até R$ 39,90) mais **2% a 2,5% de
processamento** por transação. Um sistema próprio, integrado direto a um gateway
de pagamento, elimina a taxa de 10% e dá à igreja controle sobre dados dos
inscritos, identidade visual e regras dos eventos. Detalhes e fonte em
[`docs/documentacaoProjeto.md`](docs/documentacaoProjeto.md).

## Arquitetura

Pacotes em `br.com.icb.ingressos.*` (single POM, sem multi-module):

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Domínio | `domain` | Entidades e regras de negócio. Sem dependência de frameworks. |
| Portas | `ports.in` / `ports.out` | Interfaces (contratos) de entrada e de saída. |
| Casos de uso | `usecase` | Orquestração da regra de negócio. Implementa `ports.in`, consome `ports.out`. |
| Adapters | `adapter.in` / `adapter.out` | Controllers REST, webhooks e scheduler (in); repositórios e integrações externas (out). |

Regra de dependência: `adapter` conhece `usecase` e `domain`; `domain` e
`usecase` nunca importam `adapter`.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/documentacaoProjeto.md`](docs/documentacaoProjeto.md) | Resumo consolidado, motivação e referências bibliográficas. |
| [`docs/analiseDeRequisitos.md`](docs/analiseDeRequisitos.md) | Requisitos funcionais (RF) e não funcionais (RNF). |
| [`docs/regrasDeNegocio.md`](docs/regrasDeNegocio.md) | Regras de negócio (RN-1 a RN-6) e máquinas de estado. |
| [`docs/casoUsoEArquitetura.drawio`](docs/casoUsoEArquitetura.drawio) | Diagramas de casos de uso e da arquitetura hexagonal. |
| [`docs/diagramaSequencia.drawio`](docs/diagramaSequencia.drawio) | Fluxo sequencial da compra de ingresso. |
| [`docs/entidadeRelacional.drawio`](docs/entidadeRelacional.drawio) | Modelo entidade-relacionamento. |
| [`docs/ia/agent.md`](docs/ia/agent.md) | Instruções para o assistente de IA / tech lead. |

Os arquivos `.drawio` abrem em <https://app.diagrams.net> ou na extensão
"Draw.io Integration" do VS Code / plugin do IntelliJ.

## Como compilar e executar

Pré-requisitos: **JDK 17** e **Maven 3.9+** no `PATH`.

```bash
mvn clean verify                                    # compila, testa e gera cobertura (target/site/jacoco)
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # sobe a API em http://localhost:8080 (perfil dev)
```

Os testes de integração da persistência (`*PostgresTest`) e o de concorrência da
reserva (RNF-30) rodam sobre um **PostgreSQL real e efêmero** (Zonky embarcado —
**sem Docker**); o binário nativo é baixado no primeiro `mvn test`.

### Perfis

| Perfil | Persistência | Uso |
|---|---|---|
| **`dev`** (padrão) | Em memória | Desenvolvimento local sem banco. |
| **`local`** | PostgreSQL (Neon, branch `dev`) | Rodar a API na máquina contra o banco na nuvem. Config em `application-local.yml` (fora do git). |
| **`prod`** | PostgreSQL (Neon, branch `production`) | Ambiente hospedado. Datasource via variáveis de ambiente. |

Selecione com `-Dspring-boot.run.profiles=<perfil>` ou `SPRING_PROFILES_ACTIVE`.

### Banco de dados (Épico 9)

PostgreSQL gerenciado no **Neon**. O schema é versionado em migrações **Flyway**
(`src/main/resources/db/migration`). As entidades JPA (`@Entity`) ficam na borda
(`adapter/out/persistence/jpa`), com mappers para o domínio; o núcleo do hexágono
segue sem JPA. A reserva de ingresso usa **lock pessimista** (`SELECT … FOR
UPDATE`) para não vender além do estoque sob concorrência (RNF-07).

Variáveis de ambiente do perfil `prod`:

```
JDBC_DATABASE_URL           jdbc:postgresql://<host-pooler>/<db>?sslmode=require
JDBC_DATABASE_URL_UNPOOLED   jdbc:postgresql://<host-direct>/<db>?sslmode=require   # migrações Flyway
DATABASE_USERNAME
DATABASE_PASSWORD
```

Verificação rápida com a aplicação no ar:

```bash
curl http://localhost:8080/actuator/health   # {"status":"UP"}
curl http://localhost:8080/rota-inexistente  # 404 no formato ErroResponse padrão
```

## Tecnologias

- Java 17, Maven 3.9+
- Spring Boot 3.3 (Web, Validation, Actuator, Security, Data JPA)
- Spring Security + JWT próprio (jjwt) — rotas `/api/admin/**`
- Spring Data JPA + PostgreSQL (Neon) + Flyway
- JaCoCo (cobertura), JUnit 5, Zonky embedded-postgres (testes de integração)
