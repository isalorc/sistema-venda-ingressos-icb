# Guia da API para o Frontend — ICB

Contrato REST do backend de venda de ingressos da Igreja ICB. Serve para o
frontend (repositório separado) ser desenvolvido em paralelo, inclusive sabendo
o que **ainda não existe**.

Gerado a partir dos controllers em `adapter/in/web` após o Épico 9. Fonte da
verdade em runtime: o **Swagger UI** local (`/swagger-ui.html`). Regras de
negócio em [`regrasDeNegocio.md`](regrasDeNegocio.md).

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
dados a cada restart — semeie um evento (ver [§9](#9-dados-de-teste)).

---

## 3. Autenticação

Só as rotas `/api/admin/**` exigem token (menos o próprio login). Todo o resto é
público.

| Rota | Token? |
|---|---|
| `GET /api/eventos`, `GET /api/eventos/{id}` | Não |
| `POST /api/compras` | Não |
| `POST /api/webhooks/pagamento` | Não (segredo próprio) |
| `POST /api/admin/login` | Não |
| `POST /api/admin/eventos`, `.../lotes` | **Sim** |
| `GET /api/admin/eventos/{id}/inscritos` | **Sim** |

**Fluxo:**

1. **Login** — `POST /api/admin/login` com e-mail e senha. Retorna o token e a
   validade.
2. **Guardar o token** — em memória / `sessionStorage`. É um JWT assinado, expira
   em **2 horas** (`expiraEmSegundos: 7200`). Não há refresh — expirou, faça
   login de novo.
3. **Enviar em cada chamada admin** — header `Authorization: Bearer <token>`.

Sem token (ou inválido/expirado) numa rota admin → **401** no formato padrão:

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
| `StatusIngresso` | `DISPONIVEL` · `RESERVADO` · `VENDIDO` · `UTILIZADO` | interno (não exposto hoje) |
| `StatusPagamento` | `PENDENTE` · `APROVADO` · `RECUSADO` · `CANCELADO` | interno (não exposto hoje) |

`CARTAO` sem cedilha. Valor fora da lista → **400**.

---

## 6. Endpoints — Cliente

Rotas públicas. Nenhuma exige token.

### `GET /api/eventos`

Lista os eventos **disponíveis para compra**: só os que ainda não aconteceram
*e* têm pelo menos um lote com ingresso. `menorPreco` e `ingressosDisponiveis`
consideram só os lotes com estoque. Array vazio se não houver nenhum.

**200:**

```json
[
  {
    "id": 1,
    "nome": "Congresso de Louvor 2026",
    "descricao": "Encontro anual de músicos e adoradores",
    "dataHora": "2026-12-20T19:00:00",
    "menorPreco": 60.00,
    "ingressosDisponiveis": 118
  }
]
```

### `GET /api/eventos/{eventoId}`

Detalha um evento e **todos** os seus lotes — inclusive os esgotados
(`quantidadeDisponivel: 0`) e eventos que já ocorreram. Cabe ao front decidir o
que exibir.

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
  "lotes": [
    { "id": 10, "nome": "1º Lote", "preco": 60.00, "quantidadeDisponivel": 118 },
    { "id": 11, "nome": "2º Lote", "preco": 80.00, "quantidadeDisponivel": 0 }
  ]
}
```

**Erros:** `404` evento inexistente.

### `POST /api/compras`

Inicia a compra de **um** ingresso (1 por pedido). Reserva um ingresso do lote
sob trava de concorrência, cria o pedido/pagamento pendentes e devolve os dados
de pagamento. A reserva expira em **15 minutos** se não for paga.

**Corpo:**

| Campo | Tipo | Regras |
|---|---|---|
| `loteId` | `Long` | obrigatório |
| `nome` | string | obrigatório · não vazio |
| `email` | string | obrigatório · e-mail válido |
| `telefone` | string | obrigatório · não vazio · sem máscara imposta |
| `metodoPagamento` | enum | obrigatório · `PIX` ou `CARTAO` |

```json
{
  "loteId": 10,
  "nome": "Ana Ribeiro",
  "email": "ana@exemplo.com",
  "telefone": "11988887777",
  "metodoPagamento": "PIX"
}
```

**201** — `Location: /api/pedidos/{pedidoId}`:

```json
{
  "pedidoId": 5,
  "ingressoId": 42,
  "valorTotal": 60.00,
  "statusPedido": "PENDENTE",
  "urlPagamento": "https://pagamento.fake/checkout/fake-9b1c...",
  "qrCodePix": "00020126FAKE-PIX-fake-9b1c..."
}
```

- `qrCodePix` vem preenchido só para `PIX`; para `CARTAO` é `null`.
- `urlPagamento` sempre vem, mas hoje é **sintética** (gateway fake) — ver [§8](#8-lacunas-conhecidas).
- O cliente é resolvido pelo e-mail: se já comprou antes com o mesmo e-mail,
  reaproveita o cadastro.

**Erros:** `404` lote inexistente · `409` lote esgotado · `422` campo inválido.

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

### `POST /api/admin/eventos` *(requer token)*

Cadastra um evento (ainda sem lotes).

| Campo | Tipo | Regras |
|---|---|---|
| `nome` | string | obrigatório · não vazio |
| `descricao` | string | opcional |
| `dataHora` | `LocalDateTime` | obrigatório · **no futuro** · `"2026-12-20T19:00:00"` |

**201** — `Location: /api/eventos/{id}` — corpo `{ "id": 1 }`.

**Erros:** `401` sem token · `422` nome vazio / data no passado.

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

```json
{ "nome": "1º Lote", "preco": 60.00, "quantidadeTotal": 120 }
```

**201** — `Location: /api/eventos/{eventoId}` — corpo `{ "id": 10 }`.
O `id` no corpo é o do **lote**; o `Location` aponta para o evento pai.

**Erros:** `401` sem token · `404` evento inexistente · `422` preço negativo /
quantidade ≤ 0.

### `GET /api/admin/eventos/{eventoId}/inscritos` *(requer token)*

Lista os inscritos confirmados: clientes com **pedido pago** e ingresso vendido.
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
    "dataCompra": "2026-08-30T21:14:05"
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
   `ingressosDisponiveis`.
2. **Detalhe** — `GET /api/eventos/{id}` → escolhe o lote. Desabilite no UI os
   lotes com `quantidadeDisponivel: 0`.
3. **Dados do cliente + método** — formulário: nome, e-mail, telefone,
   `PIX`/`CARTAO`.
4. **Criar a compra** — `POST /api/compras` → `201` com `pedidoId`,
   `valorTotal`, `urlPagamento`, `qrCodePix`. Trate `409` (esgotou entre o passo
   2 e agora) recarregando o detalhe.
5. **Tela de pagamento** — mostra o QR PIX (`qrCodePix`) ou manda para
   `urlPagamento`. A reserva vale 15 min.
6. **Confirmação — *aqui falta endpoint*.** Não há como o cliente consultar se o
   pedido foi pago. O pedido vira `PAGO` quando o gateway chama o webhook, e o
   ingresso é entregue por e-mail (stub). Por enquanto: mostrar "aguardando
   confirmação do pagamento — o ingresso chega no seu e-mail". Em dev/teste,
   você "confirma" chamando o webhook manualmente (Postman / curl).

> **Reserva temporária.** Entre o `201` da compra e o pagamento, o ingresso fica
> `RESERVADO` e o estoque do lote já é decrementado. Se 15 min passam sem
> pagamento, um job libera tudo de volta — o pedido vira `EXPIRADO`.

---

## 9. Lacunas conhecidas

O que **ainda não existe** no backend e afeta o frontend. Planeje as telas
sabendo disso.

- **Consulta de pedido / pagamento pelo cliente.** Não há `GET /api/pedidos/{id}`
  (o header `Location` da compra aponta para uma rota que não responde). O
  cliente não acompanha o status do pedido nem vê "meus ingressos". A tela
  pós-compra é um estado informativo, sem polling.
- **Gateway de pagamento é fake.** `urlPagamento`
  (`https://pagamento.fake/checkout/...`) e `qrCodePix` (`00020126FAKE-PIX-...`)
  são sintéticos — não abrem checkout real nem geram PIX pagável. A integração
  com o Mercado Pago (sandbox) é o **último** passo do backend. Construa a tela
  de pagamento com placeholders realistas; a forma da resposta não deve mudar.
- **E-mail é um stub.** O envio do ingresso/QR por e-mail só grava um log no
  servidor — o cliente não recebe nada. Não prometa o e-mail como canal único
  no texto da UI ainda.
- **Admin enxuto.** Um único administrador (sem tela de cadastro de admin). Sem
  editar nem excluir evento; sem editar lote; sem endpoint de check-in
  ("utilizar ingresso"). Sem paginação nas listas.

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

# 4. (cliente) comprar
curl -s -X POST localhost:8080/api/compras \
  -H 'Content-Type: application/json' \
  -d '{"loteId":10,"nome":"Ana","email":"ana@exemplo.com","telefone":"11988887777","metodoPagamento":"PIX"}'
```

**Atalho:** há uma collection do Postman em [`postman/`](postman/) que já faz
login, cria evento/lote, compra e simula o webhook — útil para gerar estado de
teste rápido.
