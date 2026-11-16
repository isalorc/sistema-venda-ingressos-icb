# Virada de lote ("lote ativo")

**Status: implementado (2026-08-31).** Complementa
[`regrasDeNegocio.md`](regrasDeNegocio.md) e [`guiaApiFrontend.md`](guiaApiFrontend.md).

Decisões desta rodada:

- **Escopo:** Fatia 1 + Fatia 2 juntas (virada por esgotamento **e** por janela de datas).
- **`inicioVendas` / `fimVendas` além da `dataHora` do evento:** é **erro 422** no
  cadastro do lote.
- **Evento na listagem pública:** aparece se for futuro **e** o `statusVendas` não
  for `ENCERRADA` (ou seja, há lote à venda ou agendado). Evento com tudo
  esgotado/encerrado some da vitrine — segue acessível por link direto.

---

## 1. Problema

Um evento pode ter vários lotes e todos com estoque ficavam compráveis ao mesmo
tempo — um cliente comprava o "1º Lote" (mais barato) mesmo já existindo o "2º".
O esperado é a **virada de lote**: só um lote à venda por vez; quando ele acaba
(por esgotamento ou por data), o próximo assume.

Casos que a igreja quer cobrir:

1. **Por quantidade** — "o 1º lote tem 100 ingressos; quando acabar, abre o 2º".
2. **Por tempo** — "o 1º lote fica 2 semanas em venda; depois vira, mesmo que
   sobre ingresso".
3. **Agendamento** — "o admin cadastra todos os lotes de uma vez, e o 3º só abre
   em novembro".

## 2. Conceito central: lote ativo é **derivado**, não guardado

Não existe operação "abrir o próximo lote", nem job, nem gatilho. "Lote ativo" é
uma **consulta** resolvida toda vez que o backend lê o evento ou recebe uma
compra (`domain/ClassificacaoDeLotes`).

> **lote ativo de um evento** = o primeiro lote (por ordem de criação, `id ASC`)
> que satisfaz **todas** as condições:
> 1. `quantidadeDisponivel > 0`
> 2. sem `inicioVendas`, **ou** `inicioVendas <= agora`
> 3. sem `fimVendas`, **ou** `agora <= fimVendas`

Consequências, de graça:

- 1º lote esgota → na requisição seguinte ele não passa na condição 1, o 2º
  assume. Ninguém "abriu" nada.
- A reserva do último ingresso do 1º lote **expira** (RN-2) → o job repõe o
  estoque e o 1º lote **volta** a ser o ativo. Sem lógica de "desfazer virada".
- `fimVendas` do 1º lote passa → ele para de passar na condição 3 → 2º assume.
- Pode não haver **nenhum** lote ativo agora (todos agendados, ou todos
  encerrados/esgotados). Estado válido — ver §4 e §6.

**Premissa:** só um lote à venda por vez, em sequência. Este modelo **não**
suporta dois lotes compráveis simultaneamente (ex.: "inteira" e "meia" ao mesmo
tempo) — para isso a regra teria de virar "todos os lotes na janela e com
estoque". Para venda escalonada por preço/data (o caso da igreja), a sequência é
o que se quer.

## 3. Modelo de dados

Dois campos **opcionais** em `lote` (migration `V2__lote_janela_de_vendas.sql`,
aditiva, sem backfill):

| Coluna | Tipo | Regra |
|---|---|---|
| `inicio_vendas` | `timestamp` (LocalDateTime, sem fuso) | opcional. Antes disso o lote é `AGENDADO`. |
| `fim_vendas` | `timestamp` (LocalDateTime, sem fuso) | opcional. Depois disso o lote é `ENCERRADO`. |

Ambos nulos (default) = comportamento anterior: o lote entra na fila só pela
ordem (`id ASC`) e pelo estoque. Cobre o caso 1 sem configurar nada.

## 4. Status derivado do lote (`StatusLote`)

Calculado, nunca persistido.

| Status | Condição | Compra? |
|---|---|---|
| `A_VENDA` | é o lote ativo do evento | **sim** |
| `AGENDADO` | `inicioVendas` no futuro | não — "abre em DD/MM" |
| `ENCERRADO` | `fimVendas` no passado | não — "encerrado" |
| `ESGOTADO` | `quantidadeDisponivel == 0` e dentro da janela | não — "esgotado" |
| `NA_FILA` | tem estoque e está na janela, mas não é o primeiro da fila | não |

Precedência: `ENCERRADO` > `AGENDADO` > `ESGOTADO` > `A_VENDA` / `NA_FILA`.

Status do evento (`StatusVendas`): `A_VENDA` (há lote ativo) · `AGENDADA` (nenhum
ativo, mas há agendado — acompanha `aberturaVendas`) · `ENCERRADA` (nada a
vender nem a abrir).

## 5. Endpoints afetados

### `POST /api/admin/eventos/{eventoId}/lotes`

Aceita `inicioVendas` e `fimVendas` (opcionais). Validações **422**:

- `fimVendas` deve estar no futuro (`@Future`).
- `fimVendas` não pode ser antes de `inicioVendas`.
- `inicioVendas` / `fimVendas` não podem ser depois da `dataHora` do evento.

Não valida sobreposição de janelas entre lotes — a regra "primeiro da fila"
resolve.

### `GET /api/eventos` (listagem pública)

- O evento aparece se for futuro **e** `statusVendas != ENCERRADA`.
- `menorPreco` e `ingressosDisponiveis` refletem o **lote ativo**; se não há
  ativo (`AGENDADA`), refletem o **próximo lote a abrir**.
- Campos novos: `statusVendas` (`A_VENDA` / `AGENDADA` / `ENCERRADA`) e
  `aberturaVendas` (data do próximo lote a abrir; `null` fora de `AGENDADA`).

### `GET /api/eventos/{eventoId}` (detalhe público)

Continua devolvendo **todos** os lotes. Cada lote ganha `status` (§4),
`inicioVendas` e `fimVendas`.

### `POST /api/compras`

Antes de reservar, valida que o `loteId` **é o lote ativo do evento**. Se não:

| Situação | Resposta | `mensagem` |
|---|---|---|
| `ESGOTADO` | `409` | "O lote N não possui ingressos disponíveis." |
| `ENCERRADO` | `409` | "As vendas deste lote foram encerradas." |
| `AGENDADO` | `409` | "As vendas deste lote ainda não começaram." |
| `NA_FILA` | `409` | "Este lote ainda não está disponível." |

O front já trata `409` recarregando o detalhe — a mensagem nova aparece de brinde.

### `GET /api/admin/eventos`

Cada item ganha `statusVendas` (§4), para o painel saber qual lote está vendendo.
O detalhe por lote o admin obtém pelo `GET /api/eventos/{id}` público.

## 6. Casos de borda

| Caso | Comportamento |
|---|---|
| Nenhum ativo, mas há `AGENDADO` | `statusVendas: "AGENDADA"` + `aberturaVendas`. Nenhum lote selecionável. |
| Tudo `ENCERRADO`/`ESGOTADO`, nada a abrir | `statusVendas: "ENCERRADA"`. Some da listagem; detalhe acessível por link direto. |
| Reserva do último ingresso do ativo expira (RN-2) | Job repõe estoque; lote volta a `A_VENDA` na requisição seguinte. Zero código novo. |
| `fimVendas` do ativo passa durante o checkout | `POST /api/compras` responde `409` "vendas encerradas". |
| Lote sem datas entre lotes com datas | Sempre dentro da janela; entra na fila só por ordem e estoque. |
| Janelas sobrepostas | Não é erro. O primeiro da fila (menor `id`) que passa nas 3 condições vence. |

## 7. Fora de escopo

- Sequenciamento **relativo** ("o lote 2 abre quando o 1 fechar, sem data").
- Múltiplos lotes à venda ao mesmo tempo (tipos de ingresso simultâneos).
- Editar `inicioVendas` / `fimVendas` de um lote já criado (não há endpoint de
  edição de lote — entra junto quando/se existir).

## 8. Impacto no frontend

Depois que a API expõe os campos:

- **Form de lote (admin)** — dois `datetime-local` opcionais ("Início das
  vendas", "Fim das vendas"); mapear os 422 novos.
- **Detalhe do evento (admin)** — etiqueta de `status` por lote.
- **Detalhe do evento (cliente)** — só o lote `A_VENDA` selecionável; `AGENDADO`
  mostra "abre em DD/MM"; `ENCERRADO`/`ESGOTADO` viram etiqueta. Sem lote
  `A_VENDA`, esconder "Continuar".
- **Home (cliente)** — usar `statusVendas`: card normal para `A_VENDA`; "vendas a
  partir de DD/MM" para `AGENDADA`; não listar `ENCERRADA`.
- **Tipos** — `StatusLote`, `StatusVendas`; `status`/`inicioVendas`/`fimVendas`
  em `Lote`; `statusVendas`/`aberturaVendas` em `EventoResumo`.
