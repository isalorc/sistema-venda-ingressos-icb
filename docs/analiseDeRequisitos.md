# Análise de Requisitos — sistema-venda-ingressos-icb

TCC do curso de Análise e Desenvolvimento de Sistemas da Fatec.

Este documento levanta os **requisitos funcionais (RF)** e **não funcionais
(RNF)** do sistema. Ele deriva de:

- [`casoUsoEArquitetura.drawio`](casoUsoEArquitetura.drawio) — atores, casos de uso e arquitetura hexagonal
- [`diagramaSequencia.drawio`](diagramaSequencia.drawio) — fluxo de compra
- [`entidadeRelacional.drawio`](entidadeRelacional.drawio) — modelo de dados
- [`regrasDeNegocio.md`](regrasDeNegocio.md) — regras de negócio (RN-1 a RN-6)
- [`documentacaoProjeto.md`](documentacaoProjeto.md) — resumo consolidado e motivação

## 1. Visão geral do produto

Sistema web para **venda de ingressos de eventos de uma única igreja (ICB)**.
Um **Cliente** consulta os eventos disponíveis, cria uma conta, compra até 5
ingressos por pedido (um nome de participante por ingresso) e paga online (PIX
ou cartão) por meio de um gateway externo. Numa área própria, acompanha seus
pedidos e vê o QR code de cada ingresso. Um **Administrador** cadastra os
eventos, gerencia os lotes de ingressos e acompanha os inscritos.

**Motivação:** hoje a igreja usa a plataforma Sympla, que cobra **taxa de serviço
de 10%** sobre o valor do ingresso (mínimo de R$ 3,99 para ingressos até
R$ 39,90) mais **taxa de processamento de 2% a 2,5%** por transação. Um sistema
próprio integrado diretamente a um gateway de pagamento elimina a taxa de serviço
de 10% e dá à igreja controle sobre dados, identidade visual e regras dos
eventos. Detalhamento e referência em
[`documentacaoProjeto.md`](documentacaoProjeto.md) (seção 2).

Escopo do MVP (abordagem incremental — ver `regrasDeNegocio.md`):

- **Não** há mapa/numeração de assentos nem múltiplos locais.
- **1 a 5 ingressos por pedido**, todos do mesmo lote, com um nome de participante
  por ingresso (RN-1).
- Cliente **cria conta** (e-mail e senha) e precisa estar logado para comprar;
  consultar eventos continua aberto (RN-4).
- Gateway de pagamento e persistência começam como implementações simplificadas
  (adapter *fake* e repositório em memória).

### 1.1. Atores

| Ator | Descrição |
|---|---|
| **Cliente** | Pessoa que compra ingressos. Consulta eventos sem login; para comprar e ver seus pedidos, entra com a própria conta (JWT de cliente). |
| **Administrador da Igreja** | Gerencia eventos, lotes e inscritos. Autenticado via JWT. |
| **Gateway de Pagamento** (sistema externo) | Mercado Pago. Gera cobrança e notifica o resultado do pagamento via webhook. |
| **Agendador (Scheduler)** (ator de sistema) | Dispara periodicamente a expiração de reservas não pagas. |

### 1.2. Prioridade (MoSCoW)

`M` = Must (MVP) · `S` = Should · `C` = Could · `W` = Won't (agora, previsto para depois)

---

## 2. Requisitos Funcionais

### 2.1. Consulta de eventos (Cliente)

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-01** | O sistema deve listar os eventos disponíveis para compra (eventos futuros com pelo menos um lote com ingressos disponíveis). | M | UC1 |
| **RF-02** | O sistema deve exibir os detalhes de um evento: nome, descrição, data/hora e a lista de lotes com nome, preço e quantidade disponível. | M | UC1 |
| **RF-03** | O sistema não deve exibir para o Cliente eventos já encerrados ou sem ingressos. | S | UC1 |

### 2.2. Compra de ingresso (Cliente)

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-04** | O sistema deve permitir ao Cliente **autenticado** iniciar a compra de **1 a 5 ingressos** de um lote, informando o **nome do participante** de cada ingresso. | M | UC2 / RN-1 / RN-4 |
| **RF-05** | O pedido deve ser vinculado à conta do Cliente identificada pelo **token de acesso**, sem pedir os dados pessoais de novo. | M | RN-4 |
| **RF-06** | O sistema deve reservar os ingressos do lote de forma segura contra concorrência (dois clientes não podem reservar o mesmo ingresso) e **tudo ou nada**: ou reserva todos os ingressos do pedido, ou nenhum. | M | Seq. 7–14 / RN-1 |
| **RF-07** | Se o lote não tiver ingressos suficientes para o pedido, o sistema deve informar **quantos restam** (ou que está esgotado) e não criar pedido. | M | Seq. "estoque insuficiente" |
| **RF-08** | Ao reservar, o sistema deve decrementar a quantidade disponível do lote e registrar um **Pedido** com status `PENDENTE` e um **Pagamento** `PENDENTE`, com valor igual ao **preço do lote multiplicado pela quantidade** de ingressos. | M | Seq. 13–14 / RN-1 |
| **RF-09** | O sistema deve solicitar ao gateway a geração de uma cobrança (PIX ou cartão) e retornar ao Cliente o link/QR de pagamento. | M | UC3 / Seq. 15–16 / RN-3 |
| **RF-10** | O ingresso reservado **não** deve ter código QR até a confirmação do pagamento. | M | RN-6 |

### 2.3. Pagamento e confirmação

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-11** | O sistema deve expor um endpoint de **webhook** para o gateway notificar o resultado do pagamento. | M | UC8 / Seq. |
| **RF-12** | O processamento da notificação de pagamento deve ser **idempotente** (notificações repetidas não podem gerar efeito duplicado). | M | RN / boas práticas |
| **RF-13** | Ao confirmar pagamento **aprovado**, o sistema deve: marcar o Pagamento como `APROVADO`, o Pedido como `PAGO`, o Ingresso como `VENDIDO` e **gerar o código QR** definitivo. | M | RN-6 |
| **RF-14** | Ao confirmar pagamento **recusado**, o sistema deve liberar a reserva do ingresso e repor a quantidade disponível do lote. | M | RN |
| **RF-15** | Após a venda confirmada, o sistema deve enviar o ingresso (com QR) ao Cliente por **e-mail**. | S | RN-4 |
| **RF-16** | O sistema deve validar a autenticidade da notificação recebida no webhook (assinatura/segredo). | M | Segurança |

### 2.4. Expiração de reservas

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-17** | O sistema deve, periodicamente, identificar pedidos `PENDENTE` criados há mais de **15 minutos** (configurável) e expirá-los. | M | RN-2 |
| **RF-18** | Ao expirar um pedido, o sistema deve marcá-lo como `EXPIRADO`, liberar o ingresso (`DISPONIVEL`), repor a quantidade do lote e cancelar o pagamento pendente. | M | RN-2 |

### 2.5. Administração (Administrador)

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-19** | O sistema deve permitir o **login** do Administrador (e-mail + senha) e retornar um token de acesso. | M | UC7 / RN-5 |
| **RF-20** | O sistema deve permitir ao Administrador **cadastrar um evento** (nome, descrição, data/hora). | M | UC4 |
| **RF-21** | O sistema deve permitir ao Administrador **editar** um evento. | S | UC4 |
| **RF-22** | O sistema deve permitir ao Administrador **criar lotes** para um evento (nome, preço, quantidade total). | M | UC5 |
| **RF-23** | Ao criar um lote, o sistema deve gerar os ingressos correspondentes com status `DISPONIVEL`. | M | Seq. / RN |
| **RF-24** | O sistema deve permitir ao Administrador **editar** um lote (preço, quantidade) respeitando ingressos já vendidos/reservados. | S | UC5 |
| **RF-25** | O sistema deve permitir ao Administrador **visualizar os inscritos** de um evento: um registro por ingresso pago, com o **nome do participante** e os dados do **comprador**. | M | UC6 |
| **RF-26** | Todas as operações administrativas devem exigir Administrador autenticado. | M | RN-5 |

### 2.6. Conta e área do Cliente

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-28** | O sistema deve permitir ao Cliente **criar uma conta** com nome, e-mail, telefone e senha (mínimo de 8 caracteres, guardada com hash BCrypt). O e-mail é único. | M | UC9 / RN-4 |
| **RF-29** | O telefone deve ser validado (DDD + celular de 9 dígitos ou fixo de 8) e guardado só com os dígitos. | M | UC9 / RN-4 |
| **RF-30** | O sistema deve permitir o **login** do Cliente (e-mail + senha) e devolver um token de acesso com validade de **7 dias**. | M | UC9 / RN-4 |
| **RF-31** | Quem comprou antes de existir conta deve conseguir **ativar a conta** cadastrando-se com o mesmo e-mail, mantendo o histórico de pedidos. | S | RN-4 |
| **RF-32** | O sistema deve listar os **pedidos do Cliente** (evento, lote, quantidade, valor, status e prazo da reserva quando pendente), do mais recente para o mais antigo. | M | UC10 |
| **RF-33** | O sistema deve exibir o **detalhe de um pedido** com cada ingresso, o nome do participante, o status e, quando pago, o **QR code em imagem**. Um Cliente não pode ver pedidos de outro. | M | UC10 / RN-6 |
| **RF-34** | Quando o prazo da reserva passar, a área do Cliente deve mostrar o pedido como **reserva expirada** imediatamente, sem esperar o job de expiração. | S | UC10 / RN-2 |

### 2.7. Validação de ingresso (check-in) — futuro

| ID | Requisito | Prioridade | Origem |
|---|---|---|---|
| **RF-27** | O sistema deve permitir validar um ingresso pelo código QR na entrada do evento, marcando-o como `UTILIZADO` e impedindo reuso. | W | `StatusIngresso.UTILIZADO` |

### 2.8. Rastreabilidade caso de uso → requisitos

| Caso de uso | Requisitos |
|---|---|
| UC1 Visualizar eventos disponíveis | RF-01, RF-02, RF-03 |
| UC2 Comprar ingresso | RF-04 … RF-10 |
| UC3 Efetuar pagamento PIX/Cartão | RF-09, RF-11 |
| UC4 Cadastrar evento | RF-20, RF-21 |
| UC5 Gerenciar lotes | RF-22, RF-23, RF-24 |
| UC6 Visualizar inscritos | RF-25 |
| UC7 Login de admin | RF-19, RF-26 |
| UC8 Receber confirmação de pagamento | RF-11 … RF-16 |
| UC9 Criar conta / entrar (Cliente) | RF-28 … RF-31 |
| UC10 Consultar meus pedidos e ingressos | RF-32, RF-33, RF-34 |

---

## 3. Requisitos Não Funcionais

### 3.1. Arquitetura e manutenibilidade

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-01** | O sistema deve seguir **Arquitetura Hexagonal (Ports & Adapters)**, com as camadas `domain`, `ports`, `usecase` e `adapter` nos pacotes `br.com.icb.ingressos.*`. | M |
| **RNF-02** | A camada `domain` não deve depender de nenhum framework (sem `@Entity`, sem anotações Spring). `domain` e `usecase` nunca importam `adapter`. Regra verificável automaticamente (ArchUnit). | M |
| **RNF-03** | O projeto deve usar um **único `pom.xml`** (sem multi-module). | M |
| **RNF-04** | O código deve seguir SOLID e Clean Code: imutabilidade por padrão, `record` para DTOs, guard clauses, nomenclatura sem abreviações, JavaDoc nas portas. | M |
| **RNF-05** | O backend deve expor **somente API REST + webhook**; o frontend é um projeto separado. | M |
| **RNF-06** | O sistema deve ser executável em Java **17**. | M |

### 3.2. Confiabilidade e integridade de dados

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-07** | A reserva de ingressos deve usar **controle de concorrência com bloqueio pessimista**, garantindo que a quantidade disponível de um lote **nunca fique negativa** e que um ingresso não seja vendido duas vezes. | M |
| **RNF-08** | As operações de compra e de confirmação de pagamento devem ser **transacionais** (tudo ou nada). | M |
| **RNF-09** | O webhook de pagamento deve ser **idempotente** e tolerante a entregas fora de ordem. | M |
| **RNF-10** | Valores monetários devem usar `BigDecimal` com escala 2; nunca `double`/`float`. | M |
| **RNF-11** | O schema do banco deve ser versionado por **migrations** (Flyway), sem alteração manual em produção. | M |

### 3.3. Segurança

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-12** | As rotas `/api/admin/**` devem exigir JWT de **administrador**, e as rotas de compra e da área do cliente (`/api/compras`, `/api/clientes/me/**`) JWT de **cliente**. O token carrega o papel (`ADMIN` ou `CLIENTE`) e um papel não acessa as rotas do outro. Senhas de administrador e de cliente com hash **BCrypt**. | M |
| **RNF-13** | Segredos (chave JWT, credenciais do gateway, segredo do webhook, credenciais de banco) devem vir de **variáveis de ambiente**, nunca versionados. | M |
| **RNF-14** | Todo o tráfego deve ocorrer sobre **HTTPS** em produção. | M |
| **RNF-15** | As entradas da API devem ser validadas (Bean Validation) e os erros retornados em formato padronizado, sem vazar stack trace. | M |
| **RNF-16** | O sistema deve aplicar **CORS** restrito ao domínio do frontend. | M |
| **RNF-17** | Rotas de compra devem ter proteção contra abuso (rate limiting) — pode ser adiada. | C |

### 3.4. Privacidade / LGPD

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-18** | O sistema coleta apenas os dados pessoais necessários do Cliente (nome, e-mail, telefone e senha) e o nome de cada participante — princípio da minimização. O CPF **não** é coletado: só se justificaria com uma regra por pessoa (limite de ingressos, meia-entrada ou entrada com documento), que a igreja não tem hoje. | M |
| **RNF-19** | A finalidade do uso dos dados (emissão dos ingressos e contato sobre os eventos) deve ser informada ao Cliente no cadastro da conta. | S |
| **RNF-20** | Deve ser possível excluir/anonimizar os dados de um Cliente mediante solicitação. | C |

### 3.5. Desempenho e escalabilidade

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-21** | A resposta do endpoint de compra (incluindo geração da cobrança) deve ocorrer em até **3 segundos** em condições normais. | S |
| **RNF-22** | A listagem de eventos deve responder em até **1 segundo**. | S |
| **RNF-23** | O sistema deve suportar picos de acesso concorrente na abertura de um lote sem inconsistência de estoque (ver RNF-07). Meta de referência: 100 requisições de compra concorrentes. | S |
| **RNF-24** | O MVP roda em **instância única**. Para múltiplas instâncias, o job de expiração precisará de trava distribuída (ex.: ShedLock) — previsto, não implementado agora. | W |

### 3.6. Disponibilidade e operação

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-25** | O sistema deve expor endpoints de **health check** e métricas (Spring Actuator). | M |
| **RNF-26** | Os logs devem ser estruturados e permitir correlacionar uma requisição do início ao fim (correlation id). | S |
| **RNF-27** | O sistema deve ser empacotado como **imagem Docker**, com `docker-compose` para app + banco. | S |
| **RNF-28** | Falha na comunicação com o gateway de pagamento não pode deixar dados inconsistentes (a reserva expira normalmente se a cobrança não for concluída). | M |

### 3.7. Qualidade e testes

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-29** | Cobertura de testes mínima de **80%** nas camadas `domain` e `usecase`. | S |
| **RNF-30** | Deve haver testes automatizados de concorrência para a reserva de ingressos. | M |
| **RNF-31** | As regras de arquitetura (RNF-02) devem ser garantidas por testes automatizados. | S |
| **RNF-32** | O build (`mvn verify`) deve rodar em pipeline de **CI** a cada push. | S |
| **RNF-33** | Testes de integração de banco devem usar **Testcontainers** (PostgreSQL real). | S |

### 3.8. Usabilidade e internacionalização

| ID | Requisito | Prioridade |
|---|---|---|
| **RNF-34** | Mensagens de erro e textos da API em **português (pt-BR)**. | M |
| **RNF-35** | Datas e valores no fuso e formato brasileiros (America/Sao_Paulo, R$). O relógio do backend usa o fuso configurado (`app.fuso-horario`), não o do servidor, para que um servidor em UTC não desloque prazos de reserva e janelas de venda. | M |
| **RNF-36** | O contrato da API deve ser documentado (OpenAPI/Swagger) para consumo pelo time de frontend. | M |

---

## 4. Restrições

| ID | Restrição |
|---|---|
| **RES-01** | Ecossistema **estritamente Java + Maven**. |
| **RES-02** | Estrutura de pastas enxuta; não criar diretórios/arquivos redundantes (ver `ia/agent.md`). |
| **RES-03** | Gateway de pagamento: **Mercado Pago** (SDK Java), com ambiente sandbox antes de produção. |
| **RES-04** | Banco de dados relacional: **PostgreSQL**. |
| **RES-05** | Projeto de TCC — prazo acadêmico; priorizar o MVP funcional sobre o escopo completo. |

## 5. Premissas

- A igreja possui uma conta no Mercado Pago para receber os pagamentos.
- Há um servidor/hospedagem com HTTPS e acesso à internet para receber webhooks.
- O volume de eventos e de vendas é compatível com uma aplicação de instância única.
- O e-mail informado pelo Cliente é válido e é o canal de entrega do ingresso.

## 6. Requisitos fora de escopo (agora)

- Recuperação de senha e confirmação de e-mail do Cliente (dependem do envio real de e-mail).
- Coleta de CPF (ver RNF-18).
- Boleto como método de pagamento (evolução de RN-3).
- Check-in/validação de ingresso na portaria (RF-27).
- Reembolso/estorno, cupons de desconto, meia-entrada.
- Relatórios financeiros e dashboards administrativos.
- Aplicativo mobile.
