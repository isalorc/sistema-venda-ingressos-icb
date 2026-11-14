# Guia da API para o Frontend — ICB

Contrato REST do backend de venda de ingressos da Igreja ICB. Serve para o
frontend (repositório separado) ser desenvolvido em paralelo, inclusive sabendo
o que **ainda não existe**.

Gerado a partir dos controllers em `adapter/in/web`. Fonte da verdade em runtime:
o **Swagger UI** local (`/swagger-ui.html`). Regras de negócio em
[`regrasDeNegocio.md`](regrasDeNegocio.md); a "virada de lote" (lote ativo,
janela de vendas) em [`viradaDeLote.md`](viradaDeLote.md); edição/cancelamento/
exclusão de evento e lote, período (`dataFim`) e imagem em
[`gestaoDeEventos.md`](gestaoDeEventos.md).

| | |
|---|---|
| Base URL (local) | `http://localhost:8080` |
| Formato | JSON · UTF-8 |
| Auth | JWT — apenas `/api/admin/**` |
| Versão | `1.0.0-SNAPSHOT` |

---

## 1. Visão geral

API REST pura, **sem estado de sessão**. O frontend consome esta API e nada
mais — sem SSR, sem cookies, sem CSRF.

- **Todas as respostas são JSON** (`Content-Type: application/json`, UTF-8).
  Envie o mesmo header nos `POST`.
- **Datas** são `LocalDateTime` ISO-8601 **sem fuso**: `"2026-12-20T19:00:00"`.
  É a hora "de parede" do evento — não converta timezone, exiba como veio. Ao
  enviar (admin), use exatamente esse formato (sem `Z`, sem offset).
- **Dinheiro** é número JSON com 2 casas: `60.00`. Nunca negativo, no máximo 2
  decimais.
- **IDs** são inteiros (`Long`), sequenciais, começam em 1.
- Campos opcionais podem vir `null` (`descricao`, `urlPagamento`, `qrCodePix`).
  Listas vazias vêm como `[]`, nunca `null`.
- Sem paginação, sem versionamento na URL, sem rate limiting (ainda).

**Duas audiências:**

- **Cliente** (público, sem login): consulta eventos e compra ingresso
  informando nome/e-mail/telefone na hora — não há cadastro.
- **Administrador da igreja** (com login): cadastra eventos e lotes, acompanha
  inscritos. Um único admin.

---

## 2. Ambientes e CORS

| Item | Local (dev) | Produção |
|---|---|---|
| Base URL | `http://localhost:8080` | a definir (deploy pendente) |
| Origem CORS liberada | `http://localhost:5173` | via env `CORS_ORIGINS` |
| Swagger UI | `/swagger-ui.html` | desligado |
| OpenAPI JSON | `/v3/api-docs` | desligado |
| Health | `/actuator/health` | `/actuator/health` |

`http://localhost:5173` é a porta padrão do Vite. Se o front rodar em outra
porta, ajuste `CORS_ORIGINS` no backend (variável de ambiente, lista separada
por vírgula) — senão o navegador bloqueia as chamadas.

CORS liberado: métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`, todos os
headers, **`allowCredentials: false`** — não use cookies; a autenticação é só
pelo header `Authorization`.

### Subir o backend localmente

```bash
# perfil dev — persistência em memória, sem banco, dados somem ao reiniciar
mvn spring-boot:run

# perfil local — persistência PostgreSQL (Neon), dados persistem
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Para desenvolver o frontend, o perfil `dev` basta e é mais rápido. Ele zera os
dados a cada restart — semeie um evento (ver [§10](#10-dados-de-teste)).

---

## 3. Autenticação

Há **dois tipos de token**, ambos JWT `Bearer`: o do **admin** e o do
**cliente**. Cada um só abre as próprias rotas — token de cliente numa rota
admin dá **403**, e vice-versa.

| Rota | Token |
|---|---|
| `GET /api/eventos`, `GET /api/eventos/{id}` | Nenhum |
| `POST /api/clientes/cadastro`, `POST /api/clientes/login` | Nenhum |
| `POST /api/compras` | **Cliente** |
| `GET /api/clientes/me`, `GET /api/clientes/me/pedidos`, `GET /api/clientes/me/pedidos/{id}` | **Cliente** |
| `POST /api/webhooks/pagamento` | Nenhum (segredo próprio) |
| `POST /api/admin/login` | Nenhum |
| demais `/api/admin/**` | **Admin** |

**Admin:**

1. **Login** — `POST /api/admin/login` com e-mail e senha.
2. **Guardar o token** — em memória / `sessionStorage`. Expira em **2 horas**
   (`expiraEmSegundos: 7200`). Sem refresh — expirou, login de novo.
3. **Enviar em cada chamada admin** — `Authorization: Bearer <token>`.

**Cliente:**

1. **Criar conta** — `POST /api/clientes/cadastro` (já devolve o token, o
   cliente sai logado) **ou entrar** — `POST /api/clientes/login`.
2. **Guardar o token** — `localStorage` é aceitável aqui. Expira em **7 dias**
   (`expiraEmSegundos: 604800`). Sem refresh.
3. **Enviar** em `POST /api/compras` e nas rotas `/api/clientes/me/**`.

Sem token (ou inválido/expirado) numa rota protegida → **401** no formato padrão:

```json
{
  "timestamp": "2026-08-30T22:39:57.331924300Z",
  "status": 401,
  "erro": "Unauthorized",
  "mensagem": "Autenticação obrigatória para esta rota.",
  "caminho": "/api/admin/eventos"
}
```

---

## 4. Erros e status

Todo erro sai no mesmo envelope `ErroResponse`:

```json
{
  "timestamp": "2026-08-30T22:39:57.331Z",
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Um ou mais campos são inválidos.",
  "caminho": "/api/compras",
  "camposInvalidos": [
    { "campo": "email", "mensagem": "E-mail inválido." },
    { "campo": "telefone", "mensagem": "O telefone é obrigatório." }
  ]
}
```

`camposInvalidos` só aparece em erros de validação (422). Nos demais o campo é
omitido — trate como ausente.

| Status | Quando | O que o front faz |
|---|---|---|
| `200` | Consulta OK | renderiza |
| `201` | Recurso criado (compra, evento, lote). Header `Location` presente. | usa o corpo; o id vem nele |
| `400` | JSON malformado, tipo errado, enum inválido | erro genérico "requisição inválida" |
| `401` | Sem token / token inválido · ou login com senha errada | redireciona pro login / "credenciais inválidas" |
| `404` | Evento / lote inexistente | tela "não encontrado" |
| `409` | Lote esgotado · ou transição de estado inválida | "ingressos esgotados" — recarrega a disponibilidade |
| `422` | Validação de campo · ou regra de negócio violada | destaca `camposInvalidos` no formulário |
| `500` | Erro inesperado no servidor | "tente novamente mais tarde" — `mensagem` é genérica de propósito |

---

## 5. Enums

Enviados e recebidos como **string em maiúsculas**, exatamente assim:

| Enum | Valores | Onde |
|---|---|---|
| `MetodoPagamento` | `PIX` · `CARTAO` | envio na compra |
| `StatusPedido` | `PENDENTE` · `PAGO` · `CANCELADO` · `EXPIRADO` | resposta da compra |
| `StatusLote` | `A_VENDA` · `AGENDADO` · `ENCERRADO` · `ESGOTADO` · `NA_FILA` | detalhe do evento (por lote) |
| `StatusVendas` | `A_VENDA` · `AGENDADA` · `ENCERRADA` | listagem de eventos e painel admin |
| `StatusIngresso` | `DISPONIVEL` · `RESERVADO` · `VENDIDO` · `UTILIZADO` | interno (não exposto hoje) |
| `StatusPagamento` | `PENDENTE` · `APROVADO` · `RECUSADO` · `CANCELADO` | interno (não exposto hoje) |

`CARTAO` sem cedilha. Valor fora da lista → **400**.

**Virada de lote:** só o lote `A_VENDA` é comprável. Quando ele esgota ou seu
`fimVendas` passa, o próximo lote (por ordem de criação) assume. Detalhe em
[`viradaDeLote.md`](viradaDeLote.md).

---

## 6. Endpoints — Cliente

Rotas públicas. Nenhuma exige token.

### `GET /api/eventos`

Lista os eventos **disponíveis para compra**: os que ainda não aconteceram *e*
cujo `statusVendas` não é `ENCERRADA` (há lote à venda ou agendado). Array vazio
se não houver nenhum.

`menorPreco` e `ingressosDisponiveis` refletem o **lote à venda**; quando o
evento está `AGENDADA`, refletem o **próximo lote a abrir** e vem `aberturaVendas`
(fora de `AGENDADA`, `aberturaVendas` é `null`).

**200:**

`dataHora` é o **início** do evento; `dataFim` é opcional (`null` = evento
pontual). `imagemUrl` é a URL do banner (`null` = sem imagem). Evento cancelado
**não aparece** nesta lista.

```json
[
  {
    "id": 1,
    "nome": "Congresso de Louvor 2026",
    "descricao": "Encontro anual de músicos e adoradores",
    "dataHora": "2026-12-20T19:00:00",
    "dataFim": "2026-12-22T22:00:00",
    "imagemUrl": "https://res.cloudinary.com/icb/image/upload/congresso.jpg",
    "menorPreco": 60.00,
    "ingressosDisponiveis": 118,
    "statusVendas": "A_VENDA",
    "aberturaVendas": null
  }
]
```

### `GET /api/eventos/{eventoId}`

Detalha um evento e **todos** os seus lotes — inclusive esgotados, agendados,
encerrados e eventos que já ocorreram. Cada lote traz o `status` derivado
(virada de lote) e a janela `inicioVendas` / `fimVendas` (`null` quando não
configurada). Só o lote `A_VENDA` deve ficar selecionável no front.

`cancelado: true` → evento cancelado: mostra "vendas encerradas", esconde a
compra (independente do `status` dos lotes).

| Path param | Tipo | |
|---|---|---|
| `eventoId` | `Long` | obrigatório |

**200:**

```json
{
  "id": 1,
  "nome": "Congresso de Louvor 2026",
  "descricao": "Encontro anual de músicos e adoradores",
  "dataHora": "2026-12-20T19:00:00",
  "dataFim": "2026-12-22T22:00:00",
  "imagemUrl": "https://res.cloudinary.com/icb/image/upload/congresso.jpg",
  "cancelado": false,
  "lotes": [
    { "id": 10, "nome": "1º Lote", "preco": 60.00, "quantidadeDisponivel": 0,
      "status": "ESGOTADO", "inicioVendas": null, "fimVendas": null },
    { "id": 11, "nome": "2º Lote", "preco": 80.00, "quantidadeDisponivel": 150,
      "status": "A_VENDA", "inicioVendas": null, "fimVendas": null },
    { "id": 12, "nome": "3º Lote", "preco": 100.00, "quantidadeDisponivel": 100,
      "status": "AGENDADO", "inicioVendas": "2026-11-01T00:00:00", "fimVendas": null }
  ]
}
```

**Erros:** `404` evento inexistente.

### `POST /api/compras` *(requer token de cliente)*

Inicia a compra de **1 a 5 ingressos** do mesmo lote (um pedido). O comprador é
o cliente do token; o corpo traz **um nome por ingresso** (participante).
Reserva os ingressos sob trava de concorrência — ou todos, ou nenhum —, cria o
pedido/pagamento pendentes e devolve os dados de pagamento. A reserva expira em
**15 minutos** se não for paga.

**Corpo:**

| Campo | Tipo | Regras |
|---|---|---|
| `loteId` | `Long` | obrigatório |
| `participantes` | `string[]` | obrigatório · 1 a 5 nomes · nenhum vazio |
| `metodoPagamento` | enum | obrigatório · `PIX` ou `CARTAO` |

```json
{
  "loteId": 10,
  "participantes": ["Ana Ribeiro", "Bruno Ribeiro"],
  "metodoPagamento": "PIX"
}
```

**201** — `Location: /api/clientes/me/pedidos/{pedidoId}`:

```json
{
  "pedidoId": 5,
  "ingressoIds": [42, 43],
  "valorTotal": 120.00,
  "statusPedido": "PENDENTE",
  "urlPagamento": "https://pagamento.fake/checkout/fake-9b1c...",
  "qrCodePix": "00020126FAKE-PIX-fake-9b1c..."
}
```

- `valorTotal` = preço do lote × quantidade de participantes.
- `qrCodePix` vem preenchido só para `PIX`; para `CARTAO` é `null`.
- `urlPagamento` sempre vem, mas hoje é **sintética** (gateway fake) — ver [§9](#9-lacunas-conhecidas).
- **O `loteId` tem de ser o lote à venda (`A_VENDA`).** Comprar de lote agendado,
  encerrado ou na fila → `409`.
- O limite de 5 por pedido é configurável no backend
  (`app.compra.max-ingressos-por-pedido`).

**Erros:**

| Status | Situação | `mensagem` |
|---|---|---|
| `401` | sem token de cliente (ou expirado) | "Autenticação obrigatória para esta rota." |
| `404` | lote inexistente | — |
| `409` | evento cancelado | "As vendas deste evento foram encerradas." |
| `409` | lote esgotado | "O lote N não possui ingressos disponíveis." |
| `409` | lote tem menos ingressos que o pedido | "Restam apenas N ingressos neste lote." |
| `409` | vendas do lote encerradas (`fimVendas` passou) | "As vendas deste lote foram encerradas." |
| `409` | lote ainda não abriu (`AGENDADO`) | "As vendas deste lote ainda não começaram." |
| `409` | lote na fila (há um lote anterior à venda) | "Este lote ainda não está disponível." |
| `422` | campo inválido / nome de participante vazio | — |
| `422` | mais participantes que o limite | "Informe de 1 a 5 participantes por pedido." |

O front já trata `409` recarregando `GET /api/eventos/{id}` — a `mensagem` nova
aparece de brinde.

---

## 6.1 Endpoints — Área do cliente

### `POST /api/clientes/cadastro` *(público)*

| Campo | Tipo | Regras |
|---|---|---|
| `nome` | string | obrigatório |
| `email` | string | obrigatório · e-mail válido · único |
| `telefone` | string | obrigatório · DDD + número: celular (9 dígitos, começa com 9) ou fixo (8 dígitos) · aceita máscara e `+55` |
| `senha` | string | obrigatório · 8 a 72 caracteres |

**201** — o cliente já sai logado:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEmSegundos": 604800 }
```

- O telefone é guardado **só com dígitos** (ex.: `13999998888`); o `GET /api/clientes/me`
  devolve nesse formato. Inválido → `422` com `camposInvalidos[].campo = "telefone"`.
- E-mail que **já tem conta** → `409` "Já existe uma conta com este e-mail.
  Entre com sua senha."
- E-mail que comprou **antes do login existir** (sem conta) → a conta é ativada
  nesse cadastro e os pedidos antigos aparecem em "Meus pedidos".

### `POST /api/clientes/login` *(público)*

Corpo `{ "email", "senha" }`. **200** com o mesmo formato do cadastro.
`401` "E-mail ou senha inválidos." (mensagem genérica de propósito).

### `GET /api/clientes/me` *(token de cliente)*

```json
{ "id": 7, "nome": "Ana Ribeiro", "email": "ana@exemplo.com", "telefone": "11988887777" }
```

### `GET /api/clientes/me/pedidos` *(token de cliente)*

Pedidos do cliente, **mais recentes primeiro**.

```json
[
  {
    "id": 5,
    "dataPedido": "2026-08-30T21:14:05",
    "status": "PENDENTE",
    "valorTotal": 120.00,
    "quantidadeIngressos": 2,
    "evento": {
      "id": 1, "nome": "Congresso de Louvor 2026",
      "dataHora": "2026-12-20T19:00:00", "dataFim": null, "imagemUrl": null,
      "loteId": 10, "nomeLote": "1º Lote"
    },
    "expiraEm": "2026-08-30T21:29:05"
  }
]
```

- `status`: `PENDENTE` · `PAGO` · `CANCELADO` (pagamento recusado) · `EXPIRADO`.
- `expiraEm` só vem em `PENDENTE` — use para a contagem regressiva da reserva.
- `evento` é `null` só em pedidos antigos cujo lote não pôde ser recuperado.

### `GET /api/clientes/me/pedidos/{pedidoId}` *(token de cliente)*

Os campos do item acima, mais o pagamento e os ingressos:

```json
{
  "id": 5, "dataPedido": "...", "status": "PAGO", "valorTotal": 120.00,
  "quantidadeIngressos": 2, "evento": { "...": "..." }, "expiraEm": null,
  "metodoPagamento": "PIX",
  "statusPagamento": "APROVADO",
  "ingressos": [
    { "id": 42, "nomeParticipante": "Ana Ribeiro", "status": "VENDIDO",
      "codigoQr": "b7c2f1e0-8a9d-4e3b-9c1a-2f4d6a8e0c11" },
    { "id": 43, "nomeParticipante": "Bruno Ribeiro", "status": "VENDIDO",
      "codigoQr": "0c5d7e21-..." }
  ]
}
```

- `codigoQr` só vem depois do pagamento aprovado (`PAGO`). Em `PENDENTE` os
  ingressos vêm `RESERVADO` sem QR.
- Em `EXPIRADO`/`CANCELADO`, `ingressos` vem **vazio** (voltaram ao estoque).
- Pedido de outro cliente → `404` (não revela que existe).

---

## 7. Endpoints — Admin

### `POST /api/admin/login` *(público)*

| Campo | Tipo | Regras |
|---|---|---|
| `email` | string | obrigatório · e-mail válido |
| `senha` | string | obrigatório |

**200:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBpY2IubG9jYWwi...",
  "tipo": "Bearer",
  "expiraEmSegundos": 7200
}
```

**Erros:** `401` `mensagem: "E-mail ou senha inválidos."` · `422` corpo inválido.

### `GET /api/admin/eventos` *(requer token)*

Lista **todos** os eventos do painel — inclusive os **sem lote**, os **já
ocorridos** e os **cancelados** —, do mais recente para o mais antigo (por id).
Array vazio se não houver eventos.

**200:**

```json
[
  {
    "id": 2,
    "nome": "Vigília de Ano Novo",
    "descricao": null,
    "dataHora": "2026-12-31T22:00:00",
    "dataFim": null,
    "imagemUrl": null,
    "cancelado": false,
    "jaOcorreu": false,
    "quantidadeLotes": 0,
    "ingressosTotais": 0,
    "ingressosDisponiveis": 0,
    "statusVendas": "ENCERRADA"
  },
  {
    "id": 1,
    "nome": "Congresso de Louvor 2026",
    "descricao": "Encontro anual",
    "dataHora": "2026-12-20T19:00:00",
    "dataFim": "2026-12-22T22:00:00",
    "imagemUrl": "https://res.cloudinary.com/icb/image/upload/congresso.jpg",
    "cancelado": false,
    "jaOcorreu": false,
    "quantidadeLotes": 2,
    "ingressosTotais": 150,
    "ingressosDisponiveis": 130,
    "statusVendas": "A_VENDA"
  }
]
```

- `ingressosTotais` / `ingressosDisponiveis` = soma de todos os lotes; a diferença
  é o que já foi reservado ou vendido.
- `statusVendas`: `A_VENDA` (vendendo) · `AGENDADA` (esperando um lote abrir) ·
  `ENCERRADA` (nada a vender — inclusive sem lote).
- `cancelado: true` → evento cancelado (segue aqui no painel; some da vitrine).
- Status por lote e janela de vendas: `GET /api/eventos/{id}` (público).

**Erros:** `401` sem token.

### `POST /api/admin/eventos` *(requer token)*

Cadastra um evento (ainda sem lotes).

| Campo | Tipo | Regras |
|---|---|---|
| `nome` | string | obrigatório · não vazio |
| `descricao` | string | opcional |
| `dataHora` | `LocalDateTime` | obrigatório · **no futuro** · início do evento |
| `dataFim` | `LocalDateTime` | opcional · ≥ `dataHora` (`null` = evento pontual) |
| `imagemUrl` | string | opcional · URL `http(s)` do banner |

**201** — `Location: /api/eventos/{id}` — corpo `{ "id": 1 }`.

**Erros:** `401` sem token · `422` nome vazio / `dataHora` no passado / `dataFim` antes de `dataHora` / `imagemUrl` sem `http`.

### `PUT /api/admin/eventos/{eventoId}` *(requer token)*

Substitui os dados editáveis do evento (não é PATCH parcial — envie todos).

| Campo | Regras |
|---|---|
| `nome` | obrigatório · não vazio |
| `descricao` | opcional (`null` limpa) |
| `dataHora` | obrigatório · início |
| `dataFim` | opcional · ≥ `dataHora` |
| `imagemUrl` | opcional · URL `http(s)` |

Mudar as datas revalida as janelas de venda dos lotes contra o novo fim do evento.

**200** com o evento no shape do detalhe (`GET /api/eventos/{id}`).
**404** inexistente · **409** evento cancelado · **422** `dataFim`/janela de lote inconsistente.

### `POST /api/admin/eventos/{eventoId}/cancelamento` *(requer token)*

Cancela o evento: some da vitrine pública, não aceita compra nova; pedidos,
pagamentos e ingressos ficam intactos. Sem corpo. Idempotente. **200**. **404** inexistente.

### `DELETE /api/admin/eventos/{eventoId}` *(requer token)*

Exclusão **real** (apaga evento, lotes e ingressos). Permitida **só se nenhum
ingresso está `RESERVADO` ou `VENDIDO`**.

**204** no sucesso · **404** inexistente · **409** `"Não é possível excluir um
evento com ingressos vendidos ou reservados. Cancele o evento."`

### `POST /api/admin/eventos/{eventoId}/lotes` *(requer token)*

Cria um lote e **gera automaticamente `quantidadeTotal` ingressos** disponíveis.
Chame uma vez por lote.

| Path param | Tipo | |
|---|---|---|
| `eventoId` | `Long` | obrigatório |

| Campo | Tipo | Regras |
|---|---|---|
| `nome` | string | obrigatório · não vazio |
| `preco` | decimal | obrigatório · ≥ 0 · máx. 2 casas |
| `quantidadeTotal` | integer | obrigatório · > 0 |
| `inicioVendas` | `LocalDateTime` | opcional · antes disso o lote fica `AGENDADO` |
| `fimVendas` | `LocalDateTime` | opcional · no futuro · ≥ `inicioVendas` · ≤ `dataHora` do evento |

Sem `inicioVendas` / `fimVendas`, o lote entra na fila só pela ordem de criação
e pelo estoque (virada por esgotamento). Não valida sobreposição de janelas
entre lotes.

```json
{
  "nome": "2º Lote",
  "preco": 80.00,
  "quantidadeTotal": 150,
  "inicioVendas": "2026-10-01T00:00:00",
  "fimVendas": "2026-10-31T23:59:59"
}
```

**201** — `Location: /api/eventos/{eventoId}` — corpo `{ "id": 10 }`.
O `id` no corpo é o do **lote**; o `Location` aponta para o evento pai.

**Erros:** `401` sem token · `404` evento inexistente · `422` preço negativo /
quantidade ≤ 0 / `fimVendas` no passado / `fimVendas` antes de `inicioVendas` /
datas de venda depois do fim do evento.

### `PUT /api/admin/eventos/{eventoId}/lotes/{loteId}` *(requer token)*

Substitui os dados editáveis do lote.

| Campo | Regras |
|---|---|
| `nome` | obrigatório · não vazio |
| `preco` | obrigatório · ≥ 0 · afeta só compras futuras (o pedido guarda o valor no momento da compra) |
| `quantidadeTotal` | obrigatório · **só pode aumentar** — o acréscimo vira estoque `DISPONIVEL` |
| `inicioVendas` | opcional · como na criação |
| `fimVendas` | opcional · no futuro · ≥ `inicioVendas` · ≤ fim do evento |

**200** com o evento no shape do detalhe.
**404** evento/lote inexistente · **409** evento cancelado **ou** tentou reduzir `quantidadeTotal` · **422** janela inconsistente.

### `DELETE /api/admin/eventos/{eventoId}/lotes/{loteId}` *(requer token)*

Apaga o lote e seus ingressos. Permitida **só se nenhum ingresso do lote está
`RESERVADO` ou `VENDIDO`**.

**204** no sucesso · **404** inexistente · **409** `"Não é possível excluir um lote com ingressos vendidos ou reservados."`

### `GET /api/admin/eventos/{eventoId}/inscritos` *(requer token)*

Lista os inscritos confirmados: **um item por ingresso vendido** de pedido pago.
`nomeParticipante` é quem vai ao evento; `nomeCliente`/`emailCliente`/
`telefoneCliente` são do comprador (o mesmo para todos os ingressos de um
pedido). `nomeParticipante` é `null` em ingressos vendidos antes da compra com
vários participantes.
Reservas pendentes e expiradas **não** entram. Array vazio se ninguém pagou.

**200:**

```json
[
  {
    "nomeCliente": "Ana Ribeiro",
    "emailCliente": "ana@exemplo.com",
    "telefoneCliente": "11988887777",
    "loteId": 10,
    "nomeLote": "1º Lote",
    "pedidoId": 5,
    "codigoQr": "b7c2f1e0-8a9d-4e3b-9c1a-2f4d6a8e0c11",
    "dataCompra": "2026-08-30T21:14:05",
    "nomeParticipante": "Bruno Ribeiro"
  }
]
```

**Erros:** `401` sem token · `404` evento inexistente.

---

## Webhook de pagamento

`POST /api/webhooks/pagamento` — **não é chamado pelo frontend.** É o gateway de
pagamento que chama, com o header `X-Webhook-Token`. É aqui que um pedido vira
`PAGO` (ou é cancelado) e o `codigoQr` do ingresso é gerado.

Documentado só para entender o modelo: **a confirmação do pagamento é
assíncrona.** O `POST /api/compras` devolve `PENDENTE`; a virada para `PAGO`
acontece depois, quando o gateway notifica.

---

## 8. Fluxo de compra

1. **Listagem** — `GET /api/eventos` → cards com `menorPreco` e
   `ingressosDisponiveis`. Use `statusVendas`: card normal para `A_VENDA`;
   "vendas a partir de DD/MM" (`aberturaVendas`) para `AGENDADA`.
2. **Detalhe** — `GET /api/eventos/{id}` → só o lote `status: "A_VENDA"` fica
   selecionável. `AGENDADO` → "abre em DD/MM"; `ENCERRADO`/`ESGOTADO` → etiqueta.
   Sem nenhum lote `A_VENDA`, esconda o botão "Continuar".
3. **Login** — comprar exige conta. Sem token, mande para login/cadastro e
   volte para o checkout depois.
4. **Participantes + método** — 1 a 5 nomes (o primeiro pode vir preenchido
   com o nome do cliente) e `PIX`/`CARTAO`. Mostre o total (preço × quantidade).
5. **Criar a compra** — `POST /api/compras` com o `loteId` do lote `A_VENDA` →
   `201` com `pedidoId`, `valorTotal`, `urlPagamento`, `qrCodePix`. Trate `409`
   (esgotou/virou/restam menos ingressos) recarregando o detalhe.
6. **Tela de pagamento** — mostra o QR PIX (`qrCodePix`) ou manda para
   `urlPagamento`. A reserva vale 15 min.
7. **Acompanhar** — `GET /api/clientes/me/pedidos/{pedidoId}` mostra o status;
   quando vira `PAGO`, cada ingresso traz o `codigoQr`. O pedido vira `PAGO`
   quando o gateway chama o webhook — em dev/teste, chame o webhook à mão
   (Postman / curl).

> **Reserva temporária.** Entre o `201` da compra e o pagamento, o ingresso fica
> `RESERVADO` e o estoque do lote já é decrementado (um por participante). Se 15 min passam sem
> pagamento, um job libera tudo de volta — o pedido vira `EXPIRADO`.

---

## 9. Lacunas conhecidas

O que **ainda não existe** no backend e afeta o frontend. Planeje as telas
sabendo disso.

- **Conta do cliente enxuta.** Sem "esqueci minha senha", sem troca de senha e
  sem editar perfil. O e-mail não é confirmado (não há envio real de e-mail):
  quem se cadastra com o e-mail de uma compra antiga, feita sem conta, passa a
  ver aqueles pedidos.
- **Retomar o pagamento.** O detalhe do pedido não devolve de novo
  `urlPagamento`/`qrCodePix`; se o cliente sair da tela de pagamento, o pedido
  pendente expira em 15 min e ele compra de novo.
- **Gateway de pagamento é fake.** `urlPagamento`
  (`https://pagamento.fake/checkout/...`) e `qrCodePix` (`00020126FAKE-PIX-...`)
  são sintéticos — não abrem checkout real nem geram PIX pagável. A integração
  com o Mercado Pago (sandbox) é o **último** passo do backend. Construa a tela
  de pagamento com placeholders realistas; a forma da resposta não deve mudar.
- **E-mail é um stub.** O envio do ingresso/QR por e-mail só grava um log no
  servidor — o cliente não recebe nada. Não prometa o e-mail como canal único
  no texto da UI ainda.
- **Admin enxuto.** Um único administrador (sem tela de cadastro de admin). Sem
  endpoint de check-in ("utilizar ingresso"). Sem paginação nas listas. Sem
  upload de imagem pelo backend (só guarda a URL — o front sobe pro Cloudinary
  ou similar). Sem "descancelar" evento. `quantidadeTotal` de lote só aumenta.
  Editar/excluir/cancelar evento e lote **já existe** (ver acima).
- **`quantidadeTotal` do lote não é exposta em nenhum GET.** O detalhe público
  (`GET /api/eventos/{id}`) só traz `quantidadeDisponivel`. Como o `PUT` de lote
  exige `quantidadeTotal` (e só deixa aumentar), a tela de edição pede o **novo
  total** ao admin e usa `quantidadeDisponivel` só como piso do campo; o `409`
  "só pode aumentar" do backend é a validação real. Se algum dia houver um
  detalhe admin do evento com `quantidadeTotal` por lote, pré-preencher o campo.

---

## 10. Dados de teste

### Credenciais do admin (dev / local)

| | |
|---|---|
| e-mail | `admin@icb.local` |
| senha | `admin123` |

Configuráveis por ambiente (`ADMIN_EMAIL`, `ADMIN_SENHA_HASH`). Em produção são
obrigatórios e não têm default.

### Semear um evento comprável

No perfil `dev` o banco começa vazio a cada restart. Sequência mínima:

```bash
# 1. login — guarde o token
curl -s -X POST localhost:8080/api/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@icb.local","senha":"admin123"}'

# 2. criar evento (troque TOKEN) -> devolve {"id": 1}
curl -s -X POST localhost:8080/api/admin/eventos \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer TOKEN' \
  -d '{"nome":"Congresso de Louvor 2026","descricao":"Anual","dataHora":"2026-12-20T19:00:00"}'

# 3. criar lote no evento 1 -> gera 120 ingressos
curl -s -X POST localhost:8080/api/admin/eventos/1/lotes \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer TOKEN' \
  -d '{"nome":"1o Lote","preco":60.00,"quantidadeTotal":120}'

# 4. (cliente) criar conta — guarde o token do cliente
curl -s -X POST localhost:8080/api/clientes/cadastro \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","email":"ana@exemplo.com","telefone":"11988887777","senha":"senha-forte"}'

# 5. (cliente) comprar 2 ingressos (troque TOKEN_CLIENTE)
curl -s -X POST localhost:8080/api/compras \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer TOKEN_CLIENTE' \
  -d '{"loteId":10,"participantes":["Ana","Bruno"],"metodoPagamento":"PIX"}'
```

**Atalho:** há uma collection do Postman em [`postman/`](postman/) que já faz
login, cria evento/lote, compra e simula o webhook — útil para gerar estado de
teste rápido.
