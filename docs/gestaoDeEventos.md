# Gestão de eventos — edição, cancelamento, exclusão, período e imagem

**Status: implementado (2026-08-31), 170 testes verdes.** Complementa
[`regrasDeNegocio.md`](regrasDeNegocio.md), [`viradaDeLote.md`](viradaDeLote.md)
e [`guiaApiFrontend.md`](guiaApiFrontend.md).

Detalhes de API já refletidos no `guiaApiFrontend.md`, no Artifact e na collection
Postman. Migração `V3__evento_periodo_imagem_cancelamento.sql` aplicada na branch
`dev` do Neon; **pendente na `production`**.

Decisões desta rodada (usuária, 2026-08-31):

- Evento passa a ter **data de início e data de fim** (fim opcional). O campo
  atual `dataHora` **permanece com esse nome** e passa a significar o início
  (o frontend já está adiantado — não vale a pena renomear); entra só o
  `dataFim`.
- Evento ganha **`imagemUrl`** (banner/pôster) — o backend só guarda a URL.
- Admin pode **editar** e **excluir** evento e lote; excluir só quando **não há
  venda nem reserva**. Com venda, o caminho é **cancelar** o evento.
- `quantidadeTotal` de lote: só **aumentar** (diminuir → 409, conflito com os
  ingressos já gerados).
- `dataHora`/`dataFim` do evento: editáveis livremente; revalidam as janelas de
  venda dos lotes.
- Evento cancelado no detalhe público: campo `cancelado: true`.
- Erros de "operação não permitida" reaproveitam o **409** já existente
  (`TransicaoInvalidaException`) — sem exceção nova.

---

## 1. Problema

Hoje o admin só **cria** evento e lote — não edita nem remove nada (lacuna
registrada no guia). E o evento tem um único instante (`dataHora`), o que não
serve para um congresso de fim de semana ou um retiro de três dias. A igreja
precisa de controle: corrigir descrição/preço, remarcar data, tirar um evento
do ar, apagar um rascunho que não vai acontecer.

## 2. Modelo de dados

### Evento

| Campo | |
|---|---|
| `data_hora` | já existe · obrigatório · **passa a significar o início** |
| `data_fim` | novo · `timestamp` **opcional** (nulo = evento pontual) |
| `imagem_url` | novo · `varchar(2048)` **opcional** |
| `cancelado_em` | novo · `timestamp` **opcional** (nulo = ativo) |

Invariantes de domínio:

- `data_fim`, quando presente, **≥ `data_hora`**.
- `imagem_url`, quando presente, tem de começar com `http://` ou `https://`.

Semântica:

- **`jaOcorreu(agora)`** passa a ser `agora > (dataFim ?? dataHora)` — um evento
  de vários dias fica "em andamento" (e listado) até o fim.
- **Janela de venda de lote** (`viradaDeLote.md` §5): `inicioVendas`/`fimVendas`
  não podem passar de **`dataFim ?? dataHora`** — não se vende ingresso para
  evento que já acabou.
- **Evento cancelado** (`canceladoEm != null`): some da listagem pública; o
  detalhe responde com `cancelado: true`; `POST /api/compras` → 409; não pode ser
  editado (só re-cancelado, que é no-op). Pedidos, pagamentos e ingressos já
  existentes ficam intactos. Reembolso é tratado fora do sistema — **não é
  escopo da API**.

Migração: `V3__evento_periodo_imagem_cancelamento.sql`
```sql
alter table evento add column data_fim      timestamp(6);
alter table evento add column imagem_url    varchar(2048);
alter table evento add column cancelado_em  timestamp(6);
```

### Imagem — onde o arquivo mora

O backend **só guarda a URL**. O upload do arquivo é responsabilidade do
frontend (ex.: Cloudinary com *unsigned upload preset*, ou Cloudflare R2). Sem
credencial de storage no backend, sem `multipart`, sem bucket.

*Evolução futura (se a banca cobrar):* `ArmazenamentoDeArquivosPort` (porta de
saída, como o gateway de pagamento) + adapter S3/R2 + `POST .../imagem`
multipart. Fora de escopo agora.

## 3. Endpoints novos

Todos exigem token de admin. Corpo `PUT` = **substituição** dos campos editáveis:
envie todos; um opcional ausente ou `null` fica `null`. Não é PATCH parcial — o
form de edição do front já carrega o objeto inteiro.

### `PUT /api/admin/eventos/{eventoId}`

| Campo | Regras |
|---|---|
| `nome` | não vazio |
| `descricao` | opcional (pode virar `null`) |
| `dataHora` | — |
| `dataFim` | opcional · ≥ `dataHora` |
| `imagemUrl` | opcional · URL `http(s)` |

Ao mudar `dataHora`/`dataFim`: revalida as janelas de venda de **todos** os
lotes. Se alguma passar a ficar depois do novo fim → **422** (`PeriodoDeVendaInvalidoException`)
nomeando o lote — a edição inteira é rejeitada.

`200` com o evento atualizado (mesmo shape do detalhe). `404` evento inexistente.
`409` evento cancelado (não se edita evento cancelado).

### `POST /api/admin/eventos/{eventoId}/cancelamento`

Marca `canceladoEm = agora`. Idempotente (cancelar de novo é no-op, `200`).
`404` inexistente. Sem corpo.

*(Descancelar não entra agora — se cancelou por engano, cria de novo.)*

### `DELETE /api/admin/eventos/{eventoId}`

Exclusão **real**: apaga o evento, seus lotes e seus ingressos em cascata.

Permitida **só se nenhum ingresso do evento está `RESERVADO` ou `VENDIDO`**
(ninguém comprou nem tem reserva ativa). Caso contrário → **409**
`"Não é possível excluir um evento com ingressos vendidos ou reservados. Cancele o evento."`

`204 No Content` no sucesso. `404` inexistente.

### `PUT /api/admin/eventos/{eventoId}/lotes/{loteId}`

| Campo | Regras |
|---|---|
| `nome` | não vazio |
| `preco` | ≥ 0 · máx. 2 casas · afeta só compras futuras (o pedido guarda o valor no momento da compra) |
| `inicioVendas` | opcional · mesmas regras da criação |
| `fimVendas` | opcional · no futuro · ≥ `inicioVendas` · ≤ fim do evento |
| `quantidadeTotal` | **só pode aumentar**. Aumentar gera `(novo − atual)` ingressos `DISPONIVEL`. Diminuir → **409** (conflita com os ingressos já gerados) |

`200` com o lote atualizado. `404` evento ou lote inexistente. `409` evento
cancelado.

### `DELETE /api/admin/eventos/{eventoId}/lotes/{loteId}`

Apaga o lote e seus ingressos em cascata. Permitida **só se nenhum ingresso do
lote está `RESERVADO` ou `VENDIDO`**. Caso contrário → **409**.

`204` no sucesso. `404` inexistente.

## 4. Regras de "é seguro mexer?"

Centralizadas no domínio, testadas isoladamente:

| Ação | Condição | Fonte |
|---|---|---|
| Excluir evento | 0 ingressos `RESERVADO`/`VENDIDO` no evento | contagem via `IngressoRepositoryPort` |
| Excluir lote | 0 ingressos `RESERVADO`/`VENDIDO` no lote | idem |
| Editar / cancelar | evento não pode estar cancelado (menos o próprio cancelamento) | `Evento.canceladoEm` |
| Diminuir `quantidadeTotal` | proibido sempre | `Lote.editado(...)` |
| Mudar data do evento | toda janela de lote continua ≤ `dataFim ?? dataHora` | `CriarLoteService` (validação compartilhada) |

Novos métodos no domínio: `Evento.cancelar(agora)`, `Evento.editado(...)`
(devolve novo `Evento` imutável), `Lote.editado(...)` (idem; recusa reduzir a
capacidade). Contagem de ingressos por status é porta de saída — novos métodos
em `IngressoRepositoryPort`: `contarPorEventoNosStatus(...)` e
`contarPorLoteNosStatus(...)`.

## 5. Impacto nos endpoints existentes

**Adicionar `dataFim`, `imagemUrl`, `cancelado` nos responses de evento** (o
campo `dataHora` continua com o mesmo nome, agora = início):

- `GET /api/eventos` (`EventoResumoResponse`): +`dataFim`, +`imagemUrl`.
  Continua escondendo evento cancelado.
- `GET /api/eventos/{id}` (`EventoDetalheResponse`): +`dataFim`, +`imagemUrl`,
  +`cancelado` (booleano). Evento cancelado responde `200` com `cancelado: true`;
  o front esconde a compra.
- `GET /api/admin/eventos` (`EventoAdminResponse`): +`dataFim`, +`imagemUrl`,
  +`cancelado`. Eventos cancelados **aparecem** aqui (é o painel de gestão).
- `POST /api/admin/eventos` (`CadastrarEventoRequest`): +`dataFim` (opcional),
  +`imagemUrl` (opcional).
- `POST /api/compras`: recusa (`409`) compra de evento cancelado.

**Ajustar validação de janela de lote** (`CriarLoteService`, reusada no
`GerenciarLoteService`): comparar com `dataFim ?? dataHora` em vez de só `dataHora`.

## 6. Impacto no frontend (repo separado)

- **Form de evento (admin)** — `datetime-local` para início; `datetime-local`
  opcional para fim; campo de imagem (upload → Cloudinary → URL).
- **Tela de edição de evento e de lote** — novas.
- **Botões** cancelar / excluir com confirmação; excluir desabilitado (ou
  escondido) quando a API responder `409` / quando `cancelado`.
- **Card e detalhe (cliente)** — mostrar intervalo de datas ("20–22 dez") e a
  imagem; badge "cancelado" some da home.
- **Tipos** — `Evento` com `dataHora` / `dataFim` / `imagemUrl` / `cancelado`;
  remover `dataHora`.

## 7. Fatiamento

**Fatia A — período e imagem do evento.** Migração V3, `Evento` com
`dataHora`/`dataFim`/`imagemUrl`, ajuste de `jaOcorreu` e da validação de
janela de lote, campos nos responses e no `POST` de criação. Sem edição ainda.

**Fatia B — edição.** `PATCH` de evento e de lote, incluindo aumento de
`quantidadeTotal`.

**Fatia C — cancelar e excluir.** `canceladoEm`, `POST .../cancelamento`,
`DELETE` de evento e de lote, regras de segurança e contadores no repositório.

## 8. Fora de escopo

- Reembolso / estorno de pagamento ao cancelar.
- Descancelar evento.
- Diminuir `quantidadeTotal` de lote.
- Backend fazer o upload da imagem (só guarda URL).
- Galeria de imagens (uma imagem por evento).
- Histórico/auditoria de quem editou o quê.
- Local do evento (endereço) — se for necessário, entra como campo simples
  depois; não muda a modelagem.
