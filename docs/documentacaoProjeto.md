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

**Épico 0 concluído** (fundação Spring Boot):

- `pom.xml`: Spring Boot 3.3.5 (`spring-boot-starter-parent`), starters `web`,
  `validation`, `actuator`, `test`; Lombok; plugins `spring-boot` e `jacoco`.
- `src/main/java/br/com/icb/ingressos/SistemaVendaIngressosApplication.java`:
  `@SpringBootApplication` no pacote raiz (component scan cobre todas as camadas).
- `src/main/resources/application.yml`: perfis `dev` (padrão, `app.persistencia=memoria`)
  e `prod` (`postgres`); Actuator expõe `health,info,metrics`; `app.reserva.ttl=PT15M` (RN-2).
- `adapter/in/web/`: `GlobalExceptionHandler` (`@RestControllerAdvice` estendendo
  `ResponseEntityExceptionHandler`) + `ErroResponse` (record) — respostas de erro
  padronizadas para 404/405/415/422/500.
- `.editorconfig` adicionado.

**Épico 1 concluído** (domínio rico):

- `domain/`: 6 entidades com comportamento e sem `@Setter` público (`@Getter` +
  `@EqualsAndHashCode` por id). Criação via fábricas estáticas `novo(...)` /
  `reconstituir(...)`; guard clauses (`Validacao`, pacote-privada) na construção.
  - `Lote`: invariante de estoque `0 <= disponivel <= total`; `reservarUnidade()` /
    `liberarUnidade()` unitários (RN-1).
  - `Ingresso`, `Pedido`, `Pagamento`: máquinas de estado de
    `docs/regrasDeNegocio.md` (`reservar` / `confirmarVenda` / `liberarReserva` /
    `utilizar`; `marcarPago` / `cancelar` / `expirar` / `reservaExpirada`;
    `aprovar` / `recusar` / `cancelar`).
  - Enums: `StatusPedido.EXPIRADO` adicionado; `MetodoPagamento.BOLETO` removido (RN-3).
- `domain/exception/`: `DominioException` (base) + `TransicaoInvalidaException`,
  `IngressoEsgotadoException`, `RecursoNaoEncontradoException`. `GlobalExceptionHandler`
  as traduz para 404 / 409 / 422.
- Testes unitários do domínio (JUnit 5 + AssertJ, sem Spring) + teste de mapeamento
  das exceções no handler.

**Épico 2 concluído** (portas):

- `ports.out/`: 8 interfaces em Java puro — repositórios (`Usuario`, `Evento`,
  `Lote` com busca sob bloqueio, `Ingresso`, `Pedido`, `Pagamento`),
  `GatewayPagamentoPort` (adapter fake no MVP) e `NotificacaoPort` (stub).
- `ports.in/`: 8 casos de uso com `record`s de comando/retorno — consulta e compra
  (cliente), confirmação de pagamento (webhook idempotente), expiração de reservas
  (scheduler) e administração (cadastrar evento, criar lote, consultar inscritos).
  Login de admin fica para o épico de segurança.
- `Pagamento` ganhou `referenciaGateway` (+ `vincularCobranca`) para o webhook
  idempotente. Hora obtida via `java.time.Clock` do JDK; código QR gerado com
  `UUID` no caso de uso (sem portas dedicadas).

**Épico 3 concluído** (casos de uso):

- `usecase/`: 3 serviços `@Service @Transactional` que implementam `ports.in` e
  consomem `ports.out`:
  - `ComprarIngressoService` — resolve/cria o cliente pelo e-mail (RN-4), reserva
    sob bloqueio (`LoteRepositoryPort#buscarPorIdComBloqueio`), cria `Pedido` e
    `Pagamento` pendentes, solicita a cobrança ao gateway e vincula a referência.
    Lote inexistente → 404; lote esgotado → 409, sem criar nada.
  - `ConfirmarPagamentoService` — processamento **idempotente** do webhook: se o
    pagamento já não está `PENDENTE`, ignora. Aprovado → `Pagamento` APROVADO,
    `Pedido` PAGO, `Ingresso` VENDIDO com QR (`UUID`), notifica o cliente.
    Recusado → libera a reserva, repõe o estoque do lote, cancela pagamento e pedido.
  - `ExpirarReservasService` — TTL de `app.reserva.ttl` (`@Value` → `Duration`);
    expira os pedidos `PENDENTE` criados antes de `agora - ttl`, devolve os
    ingressos e o estoque, cancela o pagamento pendente e retorna a contagem.
- `config/AplicacaoConfig`: bean `Clock` do JDK (sem porta dedicada — decisão do
  Épico 2), injetado nos serviços para permitir `Clock.fixed(...)` nos testes.
- `pom.xml`: adicionado `spring-tx` para `@Transactional`. Sem um
  `TransactionManager` (só entra com o JPA no Épico 9) a anotação fica inócua e
  não afeta o carregamento do contexto.

**Épico 5 concluído** (persistência em memória):

- `adapter/out/persistence/`: base pacote-privada `RepositorioEmMemoria<T>`
  (`ConcurrentHashMap` + `AtomicLong`; ao persistir entidade nova, reconstitui-a
  com o id gerado) e 6 adapters `*RepositoryEmMemoria` (`@Repository`,
  `@ConditionalOnProperty` `app.persistencia=memoria`, ativo por padrão).
- **Limitação conhecida:** `buscarPorIdComBloqueio` apenas delega para
  `buscarPorId` — não há lock pessimista real. A serialização de verdade
  (`SELECT ... FOR UPDATE`) e o teste de concorrência da reserva (RNF-30) entram
  com o JPA/PostgreSQL (Épico 9) e a suíte de qualidade (Épico 10).

**Épico 6.1 concluído** (gateway e notificação fake):

- `adapter/out/payment/GatewayPagamentoFake` (`@Component`): gera referência
  `fake-<uuid>` e dados de pagamento sintéticos (payload de QR PIX só para PIX).
  A confirmação é simulada chamando o webhook manualmente.
- `adapter/out/notification/NotificacaoPorLog` (`@Component`): *stub* que apenas
  registra em log o envio do ingresso, até a definição do template de e-mail.

**Épicos 3 (complemento), 4 e 7 concluídos** (MVP navegável):

- `usecase/`: os 5 casos de uso restantes — listar eventos disponíveis, consultar
  evento, cadastrar evento, criar lote (gera os ingressos — RF-23) e consultar
  inscritos.
- `adapter/in/web/`: `EventoController` (`GET /api/eventos`, `/{id}`),
  `CompraController` (`POST /api/compras`), `AdminEventoController`
  (`POST /api/admin/eventos`, `.../{id}/lotes`, `GET .../{id}/inscritos`) e
  `WebhookPagamentoController` (`POST /api/webhooks/pagamento`). DTOs `record` em
  `adapter/in/web/dto/`. O webhook autentica pelo header `X-Webhook-Token`
  (`app.webhook.secret`) → 401 se inválido (RF-16), traduz o status do gateway e é
  idempotente. **As rotas `/api/admin/**` ficam abertas até o Épico 8 (JWT).**
- `adapter/in/scheduler/ExpiracaoDeReservasScheduler` + `config/AgendamentoConfig`
  (`@EnableScheduling`): varre reservas vencidas a cada `app.reserva.intervalo-varredura`.
- **OpenAPI/Swagger** (RNF-36) via `springdoc`: `/swagger-ui.html` e `/v3/api-docs`
  em dev (desligados no perfil `prod`).

**Cobertura de testes:** 93 testes verdes — unitários do domínio e dos 8 serviços
(Mockito + `Clock.fixed`), 4 slices `@WebMvcTest`, teste do scheduler e dois testes
`@SpringBootTest` (`FluxoDeCompraEmMemoriaTest` e `FluxoDeCompraViaRestTest`, este
último exercitando cadastro → compra → webhook → inscrito inteiramente por HTTP).

**Ainda pendente:**

- Segurança das rotas admin (JWT) e CORS — Épico 8.
- Collection Postman (`docs/postman/`) — próximo passo.

## 11. Como compilar e executar

Pré-requisitos: **JDK 17** e **Maven 3.9+** no `PATH`.

```bash
mvn clean verify                                    # compila, testa, gera cobertura (target/site/jacoco)
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # sobe a API em http://localhost:8080
```

Verificação: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.
Contrato da API: <http://localhost:8080/swagger-ui.html> (perfil `dev`).

## 12. Próximos passos (backlog técnico)

O backlog completo (Épicos 0 a 11, sequência de execução e marco de MVP) está no
plano de desenvolvimento. Resumo:

- **Épico 0 (concluído):** Spring Boot no `pom.xml`, `main` como `@SpringBootApplication`, `application.yml` (perfis dev/prod), tratamento global de erros, JaCoCo.
- **Épico 1 (concluído):** entidades anêmicas → modelo rico (transições de estado, invariantes de estoque, exceções de domínio ligadas ao `GlobalExceptionHandler`) + testes unitários sem Spring.
- **Épico 2 (concluído):** portas de entrada (`ports.in`) e de saída (`ports.out`), incluindo `GatewayPagamentoPort` e busca com bloqueio.
- **Épico 3 (concluído):** casos de uso — os 3 serviços de fluxo (`ComprarIngressoService`, `ConfirmarPagamentoService` idempotente, `ExpirarReservasService`) e os 5 de consulta/administração.
- **Épico 5 (concluído):** persistência em memória (`RepositorioEmMemoria` + 6 adapters `*RepositoryEmMemoria`). Lock pessimista real adiado para o Épico 9.
- **Épico 6.1 (concluído):** `GatewayPagamentoFake` e `NotificacaoPorLog` (stub).
- **Épicos 4 e 7 (concluídos):** controllers REST + webhook (autenticado por segredo, idempotente), scheduler de expiração e contrato OpenAPI/Swagger → **MVP navegável em `dev`**. Rotas `/api/admin/**` abertas até o Épico 8.
- **Collection Postman** (`docs/postman/`) — **próximo passo:** environment com variáveis e credenciais (`baseUrl`, `webhookSecret`, `adminToken`…), todas as requests (compra, consulta, webhook, admin) e um exemplo de response salvo por request. Estendida junto com os Épicos 7 e 8.
- **Épicos 8–11:** segurança JWT, PostgreSQL + Flyway, qualidade/CI (ArchUnit, Jacoco, Testcontainers), Mercado Pago real, deploy.
- **Frontend:** repositório **separado**, trilha paralela que arranca quando houver um contrato estável para consumir (contrato do Épico 4 + collection Postman). Backend e frontend têm **igual peso** na entrega do TCC — trabalhar o backend primeiro neste repo é apenas sequenciamento.

## 13. Referências bibliográficas

- SYMPLA. **Quanto custa? Conheça as taxas da Sympla.** Disponível em:
  <https://produtores.sympla.com.br/quanto-custa/>. Acesso em: 28 ago. 2026.
