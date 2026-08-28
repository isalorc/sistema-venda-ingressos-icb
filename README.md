# Sistema de venda de ingressos ICB

API para gerenciar a venda de ingressos de eventos de uma igreja (ICB).
TCC do curso de Análise e Desenvolvimento de Sistemas da Fatec.

Backend em Java com **Arquitetura Hexagonal (Ports & Adapters)**. Este
repositório entrega apenas a **API REST + webhook de pagamento**; o frontend fica
em projeto separado.

## Arquitetura

Pacotes em `br.com.icb.ingressos.*` (single POM, sem multi-module):

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Domínio | `domain` | Entidades e regras de negócio. Sem dependência de frameworks. |
| Portas | `ports.in` / `ports.out` | Interfaces (contratos) de entrada e de saída. |
| Casos de uso | `usecase` | Orquestração da regra de negócio. Implementa `ports.in`, consome `ports.out`. |
| Adapters | `adapter.in` / `adapter.out` | Controllers REST e webhooks (in); repositórios e integrações externas (out). |

Regra de dependência: `adapter` conhece `usecase` e `domain`; `domain` e
`usecase` nunca importam `adapter`.

## Documentação

- [`docs/analiseDeRequisitos.md`](docs/analiseDeRequisitos.md) — requisitos funcionais e não funcionais
- [`docs/regrasDeNegocio.md`](docs/regrasDeNegocio.md) — regras de negócio (RN-1 a RN-6)
- [`docs/desenhoArquiteturaHexagonal.md`](docs/desenhoArquiteturaHexagonal.md) — visão da arquitetura
- [`docs/diagramaCasoDeUso.md`](docs/diagramaCasoDeUso.md) — casos de uso
- [`docs/diagramaSequencia.md`](docs/diagramaSequencia.md) — fluxo de compra
- [`docs/modelagemEntidadeRelacionamento.md`](docs/modelagemEntidadeRelacionamento.md) — modelo de dados
- [`docs/documentacaoProjeto.md`](docs/documentacaoProjeto.md) — resumo consolidado

## Como compilar e testar

```bash
mvn clean verify
```

## Tecnologias

- Java 17, Maven
- Spring Boot 3 (Web, Data JPA, Security) — em adoção (ver Épico 0 do backlog)
- PostgreSQL + Flyway (persistência final; MVP usa adapter em memória)
- JUnit 5
