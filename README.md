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

## Como compilar e testar

```bash
mvn clean verify
```

## Tecnologias

- Java 17, Maven
- Spring Boot 3 (Web, Data JPA, Security) — em adoção (ver Épico 0 do backlog)
- PostgreSQL + Flyway (persistência final; MVP usa adapter em memória)
- JUnit 5
