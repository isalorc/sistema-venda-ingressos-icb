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
- **Frontend:** projeto/repositório separado (`sistema-venda-ingressos-icb-web`,
  Vue 3 + TypeScript). Este repositório entrega apenas a API REST + webhook de
  pagamento.
- **Gateway de pagamento:** porta de saída com adapter *fake* no MVP; integração
  real com o Mercado Pago (sandbox) em etapa posterior.
- **Persistência:** adapter em memória no perfil `dev` e JPA/PostgreSQL (Neon) +
  Flyway + lock pessimista no perfil `prod`, escolhidos por `app.persistencia`.
- **Single POM:** um único `pom.xml` na raiz, sem multi-module.
- **Autenticação:** JWT próprio para o administrador (2 h) e para o cliente
  (7 dias), com a mesma chave e um *claim* `papel` (`ADMIN` / `CLIENTE`) que
  impede um token de acessar as rotas do outro. Senhas com hash BCrypt.
- **Relógio:** `Clock` no fuso da igreja (`app.fuso-horario`, America/Sao_Paulo),
  independente do fuso do servidor.

## 4. Regras de negócio

Acordadas com o time e detalhadas em [`regrasDeNegocio.md`](regrasDeNegocio.md)
(RN-1 a RN-6): 1 a 5 ingressos por pedido com um nome de participante por
ingresso, TTL de reserva de 15 min, pagamento PIX/cartão, conta obrigatória do
cliente para comprar, autenticação admin via JWT e geração do QR (um por
ingresso) somente após o pagamento aprovado. A virada de lote e a gestão de
eventos estão em [`viradaDeLote.md`](viradaDeLote.md) e
[`gestaoDeEventos.md`](gestaoDeEventos.md).

## 5. Requisitos

A análise de requisitos funcionais (RF-01 a RF-34) e não funcionais (RNF-01 a
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
| [`casoUsoEArquitetura.drawio`](casoUsoEArquitetura.drawio) | Páginas "Casos de Uso" (UC1 a UC10, incluindo conta e área do cliente) e "Arquitetura Hexagonal". |
| [`diagramaSequencia.drawio`](diagramaSequencia.drawio) | Fluxo sequencial da compra (`POST /api/compras`): token do cliente, bloqueio do lote, reserva de N ingressos e cobrança. |
| [`entidadeRelacional.drawio`](entidadeRelacional.drawio) | Modelo entidade-relacionamento conforme as migrações V1 a V4, com as colunas novas da área do cliente destacadas. |

## 8. Modelo de dados (ER simplificado)

Entidades principais: `USUARIO`, `EVENTO`, `LOTE`, `INGRESSO`, `PEDIDO`,
`PAGAMENTO`. Relações-chave: `USUARIO` realiza `PEDIDO`; `PEDIDO` contém
`INGRESSO`s e tem `PAGAMENTO`(s); `EVENTO` possui `LOTE`s; `LOTE` gera
`INGRESSO`s. `LOCAL` e `ASSENTO` foram removidos (escopo de igreja única
simplificado). Detalhe em [`entidadeRelacional.drawio`](entidadeRelacional.drawio).

A migração `V4__conta_cliente_e_participante.sql` acrescentou: `usuario.senha_hash`
(conta do cliente), `ingresso.nome_participante` (um nome por ingresso),
`pedido.lote_id` e `pedido.quantidade_ingressos` (o pedido guarda o evento e a
quantidade mesmo depois que a reserva expira e os ingressos voltam ao lote), além
do índice `ix_pedido_usuario_data` para a lista "Meus pedidos".

## 9. Fluxo de compra (resumo)

Cliente logado → `JwtFilter` (valida o token de cliente) → `CompraController`
(adapter/in) → `ComprarIngressoService` (core) → portas de repositório e
`GatewayPagamentoPort` (adapter/out). Inclui bloqueio de concorrência (lock
pessimista no lote), reserva tudo ou nada de 1 a 5 ingressos (um nome cada),
cobrança do valor total (link/QR) e retorno ao cliente com HTTP 201. Sem estoque
suficiente, responde 409 informando quantos restam. Detalhe em
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
  idempotente.
- `adapter/in/scheduler/ExpiracaoDeReservasScheduler` + `config/AgendamentoConfig`
  (`@EnableScheduling`): varre reservas vencidas a cada `app.reserva.intervalo-varredura`.
- **OpenAPI/Swagger** (RNF-36) via `springdoc`: `/swagger-ui.html` e `/v3/api-docs`
  em dev (desligados no perfil `prod`).

**Épico 8 concluído** (segurança admin — RF-19 / RF-26 / RNF-12 / RNF-16):

- `POST /api/admin/login` (e-mail + senha BCrypt) devolve um **JWT** próprio
  (jjwt, HMAC). Credencial de um administrador em variáveis de ambiente
  (`app.admin.*`); em dev, `admin@icb.local` / `admin123`.
- `config/SecurityConfig`: API stateless; `/api/admin/**` (exceto o login) exige
  o token via `Authorization: Bearer` (papel `ROLE_ADMIN`); consulta, compra,
  webhook, health e Swagger seguiam **públicos** (a compra passou a exigir login
  do cliente na área do cliente — ver abaixo). 401/403 no mesmo formato de erro.
- **CORS** restrito a `app.cors.allowed-origins` (env; dev `http://localhost:5173`).
- Porta `ports.out/TokenAdminPort` (impl `adapter/out/security/JwtTokenAdmin`)
  isola a biblioteca de token.

**Épico 9 concluído** (PostgreSQL — RNF-07 / RNF-11):

- `adapter/out/persistence/jpa/`: entidades JPA separadas do domínio, repositórios
  Spring Data e adapters `@ConditionalOnProperty(app.persistencia=postgres)`.
  `buscarPorIdComBloqueio` faz `SELECT ... FOR UPDATE` (lock pessimista real).
- Flyway com as migrações `V1` a `V4` em `src/main/resources/db/migration`.
  Banco de produção no **Neon** (PostgreSQL gerenciado).
- Testes de banco com **PostgreSQL embutido (Zonky)**, sem Docker:
  `FluxoDeCompraPostgresTest` e `ReservaConcorrentePostgresTest` (concorrência
  da reserva — RNF-30).

**Virada de lote e gestão de eventos concluídas** (detalhes em
[`viradaDeLote.md`](viradaDeLote.md) e [`gestaoDeEventos.md`](gestaoDeEventos.md)):

- Lote com janela de vendas (`inicio_vendas` / `fim_vendas`); só o **lote ativo**
  vende, e o próximo assume quando o anterior esgota ou a janela fecha (V2).
- Evento com data de fim, imagem e cancelamento; o admin edita, cancela e exclui
  eventos e lotes, e lista todos os eventos (V3).

**Área do cliente concluída** (RN-1 / RN-4 / RF-28 a RF-34):

- **Conta do cliente:** `POST /api/clientes/cadastro` e `POST /api/clientes/login`
  devolvem um JWT de cliente (`papel=CLIENTE`, 7 dias; porta `TokenClientePort`,
  impl `JwtTokenCliente`). Senha mínima de 8 caracteres com BCrypt. Telefone
  validado e normalizado (`domain/Telefone`: DDD + celular ou fixo, só dígitos).
  Quem já tinha comprado sem conta ativa a conta com o mesmo e-mail.
- **Segurança:** `JwtFilter` aceita os dois tipos de token e define o papel;
  `/api/compras` e `/api/clientes/me/**` exigem `ROLE_CLIENTE`, `/api/admin/**`
  exige `ROLE_ADMIN`. O token de admin passou a carregar `papel=ADMIN` e um token
  de cliente recebe 403 nas rotas do admin.
- **Compra de vários ingressos:** `POST /api/compras` recebe `participantes`
  (1 a `app.compra.max-ingressos-por-pedido`, padrão 5). A reserva é tudo ou nada
  sob o lock do lote; sem estoque suficiente, 409 com quantos restam.
- **Meus pedidos:** `GET /api/clientes/me`, `GET /api/clientes/me/pedidos` e
  `GET /api/clientes/me/pedidos/{id}` (`ConsultarPedidosClienteService`), com o
  evento do pedido, o prazo da reserva e cada ingresso com nome e QR. Pedido de
  outro cliente → 404.
- **Inscritos do admin:** um registro por ingresso, com o nome do participante e
  os dados do comprador.
- **Relógio no fuso da igreja:** `AplicacaoConfig` cria o `Clock` com
  `app.fuso-horario` (America/Sao_Paulo); antes usava o fuso do servidor, o que
  num servidor em UTC adiantaria em 3 h os prazos de reserva e as janelas de venda.

**Cobertura de testes:** 209 testes verdes — domínio (incluindo `TelefoneTest`),
serviços (`ComprarIngressoServiceTest`, `ContaClienteServiceTest`,
`ConsultarPedidosClienteServiceTest` e os demais), slices `@WebMvcTest`,
scheduler, `JwtTokenAdminTest`, `JwtTokenClienteTest` (inclusive a rejeição de
token do papel errado), testes com PostgreSQL embutido e `@SpringBootTest`
(`FluxoDeCompraViaRestTest` ponta a ponta por HTTP: 401 sem token, 403 com token
de cliente no admin, telefone inválido → 422, compra de 2 ingressos, meus pedidos
e um QR por ingresso; `SegurancaAdminTest` para login, token válido e preflight
CORS).

**Collection Postman:** `docs/postman/` — collection (todas as requests, incluindo
**Admin · Login**, com scripts que encadeiam variáveis e token e um exemplo de
response por request), environment `dev` e `README.md`. Validada com `newman`.
Inclui **Cliente — Criar conta** (guarda o `clienteToken`) e **Cliente — Meus
pedidos**; as compras usam o token do cliente.

**Ainda pendente:**

- Integração real com o Mercado Pago (hoje `GatewayPagamentoFake`) e envio real
  de e-mail (hoje `NotificacaoPorLog`).
- Recuperação de senha e confirmação de e-mail do cliente. Sem a confirmação,
  quem souber o e-mail de um comprador antigo sem conta consegue ativar essa conta.
- Retomar o link/QR de pagamento a partir do detalhe do pedido.
- Check-in pelo QR na entrada (RF-27) e faturamento/relatórios para o admin.

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
- **Épicos 4 e 7 (concluídos):** controllers REST + webhook (autenticado por segredo, idempotente), scheduler de expiração e contrato OpenAPI/Swagger → **MVP navegável em `dev`**.
- **Épico 8 (concluído):** login do administrador (`POST /api/admin/login` → JWT próprio, senha BCrypt), proteção das rotas `/api/admin/**` e CORS restrito.
- **Collection Postman** (`docs/postman/`) **concluída:** environment, todas as requests (login, compra, consulta, webhook, admin) com um exemplo de response salvo por request. Validada com `newman`.
- **Épico 9 (concluído):** PostgreSQL (Neon) + Flyway + lock pessimista real, com testes em PostgreSQL embutido (Zonky).
- **Virada de lote e gestão de eventos (concluídas):** lote ativo com janela de vendas; editar, cancelar e excluir eventos e lotes.
- **Área do cliente (concluída):** conta e login do cliente, compra de 1 a 5 ingressos com nome por participante, meus pedidos e QR por ingresso.
- **Próximos:** Mercado Pago real (sandbox), envio de e-mail, recuperação de senha, faturamento para o admin, check-in pelo QR, ArchUnit e deploy.
- **Frontend:** repositório **separado** (`sistema-venda-ingressos-icb-web`, Vue 3 + TypeScript), já com vitrine, área do cliente e painel admin. Backend e frontend têm **igual peso** na entrega do TCC.

## 13. Referências bibliográficas

- SYMPLA. **Quanto custa? Conheça as taxas da Sympla.** Disponível em:
  <https://produtores.sympla.com.br/quanto-custa/>. Acesso em: 28 ago. 2026.
