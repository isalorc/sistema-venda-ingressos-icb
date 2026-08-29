# Projeto: sistema-venda-ingressos-icb

Resumo consolidado da documentação disponível em `/docs`.

TCC do curso de Análise e Desenvolvimento de Sistemas da Fatec — desenvolvido de
forma incremental (MVP micro primeiro, depois evolução).

---

## 1. Visão geral

Sistema de venda de ingressos para os eventos de **uma igreja específica** (ICB).
Escopo limitado: sem múltiplos locais e sem numeração de assentos. Backend em
Java (Maven, Java 17) com **Arquitetura Hexagonal (Ports & Adapters)**. Este
repositório entrega apenas a **API REST + webhook de pagamento**; o frontend é um
projeto separado.

## 2. Motivação e justificativa (por que um site próprio)

Atualmente a igreja vende seus ingressos pela plataforma **Sympla**. O custo
dessa intermediação é o principal motivador para desenvolver uma solução própria.

Condições da Sympla (ver [Referências](#13-referências-bibliográficas)):

| Item | Valor |
|---|---|
| Eventos gratuitos | Sem custo (criação, publicação, gestão e distribuição de ingressos). |
| Taxa de serviço | **10%** por ingresso vendido em eventos pagos. Pode ser absorvida pelo produtor ou repassada ao comprador no checkout. |
| Taxa de serviço mínima | **R$ 3,99** por ingresso, para ingressos de valor igual ou inferior a **R$ 39,90**. |
| Taxa de processamento | **2% a 2,5%** por transação, variando conforme meio de pagamento, localidade e processador. **Não** pode ser repassada ao comprador — apenas a taxa de serviço de 10% é repassável. |
| Eventos com assento marcado | 15% ou mais (mediante negociação) — fora do escopo deste projeto. |
| Repasse ao organizador | Automático no 3º dia útil após o término do evento. |

**Exemplo ilustrativo:** em um evento com ingresso a R$ 50,00 e 200 ingressos
vendidos, a taxa de serviço de 10% representa R$ 5,00 por ingresso — R$ 1.000,00
no total — que sai do caixa da igreja (se absorvida) ou é cobrada do cliente (se
repassada). Somam-se a isso 2% a 2,5% de processamento sobre cada transação, que
oneram o organizador.

**Com um sistema próprio** integrado diretamente a um gateway de pagamento
(Mercado Pago), a igreja elimina a taxa de serviço de 10% e passa a arcar apenas
com a tarifa do meio de pagamento (tipicamente menor, sobretudo no Pix). Além da
economia, ganha controle sobre os dados dos inscritos, a identidade visual e as
regras de negócio dos eventos. Este é o objetivo central do projeto.

> Os percentuais refletem as condições públicas da Sympla na data de elaboração
> deste documento e podem variar conforme plano contratado e promoções. Servem
> como base de comparação, não como valores contratuais.

## 3. Decisões técnicas

- **Stack backend:** Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security).
  O core do hexágono (`domain`, `usecase`) permanece livre de anotações de
  framework.
- **Frontend:** projeto/repositório separado. Este repositório entrega apenas a
  API REST + webhook de pagamento.
- **Gateway de pagamento:** porta de saída com adapter *fake* no MVP; integração
  real com o Mercado Pago (sandbox) em etapa posterior.
- **Persistência:** adapter em memória no MVP para validar os casos de uso;
  depois JPA/PostgreSQL + Flyway + lock pessimista.
- **Single POM:** um único `pom.xml` na raiz, sem multi-module.
- **Autenticação admin:** JWT próprio, senhas com hash BCrypt.

## 4. Regras de negócio

Acordadas com o time e detalhadas em [`regrasDeNegocio.md`](regrasDeNegocio.md)
(RN-1 a RN-6): 1 ingresso por pedido, TTL de reserva de 15 min, pagamento
PIX/cartão, identificação do cliente sem cadastro, autenticação admin via JWT e
geração do QR somente após o pagamento aprovado.

## 5. Requisitos

A análise de requisitos funcionais (RF-01 a RF-27) e não funcionais (RNF-01 a
RNF-36), com restrições, premissas e itens fora de escopo, está em
[`analiseDeRequisitos.md`](analiseDeRequisitos.md).

## 6. Regras arquiteturais (do agente/IA)

Detalhe em [`ia/agent.md`](ia/agent.md).

- **Single POM:** tudo gerenciado no `pom.xml` raiz; não usar multi-module.
- **Pacotes:** `br.com.icb.ingressos.{domain, ports.in, ports.out, usecase, adapter.in, adapter.out, config}`.
- `domain`: entidades puras, sem dependência de frameworks (sem anotações JPA/Spring).
- `ports`: apenas interfaces (contratos).
- `usecase`: orquestração da lógica de negócio; implementa as portas de entrada, consome as de saída.
- `adapter`: subdividido em `adapter.in` (controllers, webhooks, scheduler) e `adapter.out` (repositórios, clientes externos).
- **Regra de dependência:** adapters conhecem `usecase` e `domain`; `usecase`/`domain` nunca importam `adapter`.
- **Qualidade:** SOLID, imutabilidade, `record` para DTOs, Java moderno, guard clauses. `BigDecimal` para dinheiro.
- **Comportamento da IA:** perguntar quando houver ambiguidade; não inventar regras financeiras nem campos sem confirmação.

## 7. Diagramas

Arquivos editáveis no [draw.io](https://app.diagrams.net):

| Arquivo | Conteúdo |
|---|---|
| [`casoUsoEArquitetura.drawio`](casoUsoEArquitetura.drawio) | Páginas "Casos de Uso" e "Arquitetura Hexagonal". |
| [`diagramaSequencia.drawio`](diagramaSequencia.drawio) | Fluxo sequencial da compra de ingresso. |
| [`entidadeRelacional.drawio`](entidadeRelacional.drawio) | Modelo entidade-relacionamento. |

## 8. Modelo de dados (ER simplificado)

Entidades principais: `USUARIO`, `EVENTO`, `LOTE`, `INGRESSO`, `PEDIDO`,
`PAGAMENTO`. Relações-chave: `USUARIO` realiza `PEDIDO`; `PEDIDO` contém
`INGRESSO`s e tem `PAGAMENTO`(s); `EVENTO` possui `LOTE`s; `LOTE` gera
`INGRESSO`s. `LOCAL` e `ASSENTO` foram removidos (escopo de igreja única
simplificado). Detalhe em [`entidadeRelacional.drawio`](entidadeRelacional.drawio).

## 9. Fluxo de compra (resumo)

Cliente → `IngressoController` (adapter/in) → `ComprarIngressoUseCase` (core) →
`IngressoRepositoryPort` (adapter/out) → `GatewayPagamentoPort`. Inclui bloqueio
de concorrência (lock pessimista), reserva do ingresso, integração com o
pagamento (gerar link/QR) e retorno ao cliente. Detalhe em
[`diagramaSequencia.drawio`](diagramaSequencia.drawio).

## 10. Estado atual do código

- `pom.xml`: apenas JUnit 5 e Lombok. **Spring Boot ainda não foi adicionado.**
- `src/main/java/br/com/icb/ingressos/application/SistemaVendaIngressosApplication.java`:
  `main` que apenas imprime uma mensagem. **Ainda não é `@SpringBootApplication`.**
- `src/main/java/br/com/icb/ingressos/domain/`: 6 entidades **anêmicas**
  (`@Getter/@Setter` via Lombok) — `Usuario`, `Evento`, `Lote`, `Ingresso`,
  `Pedido`, `Pagamento` — e os enums em `domain/enums/` (`StatusIngresso`,
  `StatusPedido`, `StatusPagamento`, `MetodoPagamento`).
- Ainda **não existem** os pacotes `ports`, `usecase`, `adapter`, `config`.

## 11. Como compilar e executar

```bash
mvn clean verify        # compila e roda os testes
mvn compile             # apenas compila
```

Após o Épico 0 (adoção do Spring Boot): `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

## 12. Próximos passos (backlog técnico)

O backlog completo (Épicos 0 a 11, sequência de execução e marco de MVP) está no
plano de desenvolvimento. Resumo:

- **Épico 0:** configurar Spring Boot no `pom.xml`, converter o `main` em `@SpringBootApplication`, `application.yml` (perfis dev/prod), tratamento global de erros.
- **Épico 1:** transformar as entidades anêmicas em modelo rico (transições de estado, invariantes de estoque, exceções de domínio) + testes.
- **Épico 2:** portas de entrada (`ports.in`) e de saída (`ports.out`), incluindo `GatewayPagamentoPort` e busca com bloqueio.
- **Épico 3:** casos de uso — núcleo em `ComprarIngressoService`, `ConfirmarPagamentoService` (idempotente) e `ExpirarReservasService`.
- **Épicos 4–7:** adapter REST, persistência em memória, gateway fake, scheduler de reservas → MVP navegável.
- **Épicos 8–11:** segurança JWT, PostgreSQL + Flyway, qualidade/CI (ArchUnit, Jacoco, Testcontainers), Mercado Pago real, deploy.

## 13. Referências bibliográficas

- SYMPLA. **Quanto custa? Conheça as taxas da Sympla.** Disponível em:
  <https://produtores.sympla.com.br/quanto-custa/>. Acesso em: 28 ago. 2026.
