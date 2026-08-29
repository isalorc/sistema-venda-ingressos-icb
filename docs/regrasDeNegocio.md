# Regras de Negócio — sistema-venda-ingressos-icb

Documento de referência das regras de negócio acordadas com o time. Serve de
base para os casos de uso (`usecase`) e para o modelo de domínio (`domain`).

> **Contexto:** projeto de TCC do curso de Análise e Desenvolvimento de Sistemas
> da Fatec. A estratégia é **incremental**: primeiro um MVP micro, funcional e
> bem testado; depois a evolução para o escopo macro. A coluna "Evolução futura"
> abaixo registra o alvo de longo prazo, mas **não** faz parte do MVP.

## Escopo do sistema

- Venda de ingressos para **uma única igreja** (ICB).
- **Sem** múltiplos locais e **sem** mapa/numeração de assentos.
- Atores: **Cliente** (compra ingresso) e **Administrador da Igreja**
  (cadastra eventos e lotes, acompanha inscritos).
- Pagamento processado por **gateway externo** (Mercado Pago), integrado via
  porta de saída. No MVP o adapter é **fake**; a integração real (sandbox) é
  etapa posterior.

## Tabela de regras

| # | Tema | Regra no MVP | Evolução futura |
|---|---|---|---|
| **RN-1** | Ingressos por compra | **1 ingresso por pedido.** Cada operação de compra gera um `Pedido` com exatamente um `Ingresso`. | Até **N** ingressos por pedido, com limite máximo configurável. |
| **RN-2** | Expiração da reserva (TTL) | Reserva não paga expira **15 minutos** após a criação do `Pedido` (status `PENDENTE`). Valor configurável em `application.yml`. Um job periódico libera as reservas expiradas. | — |
| **RN-3** | Métodos de pagamento | **PIX** e **CARTÃO**. Enum `MetodoPagamento = { PIX, CARTAO }`. | Adicionar **BOLETO** (compensação D+1, TTL de reserva próprio). |
| **RN-4** | Identificação do cliente | **Sem cadastro/senha.** Na compra o cliente informa **nome, e-mail e telefone**. O sistema cria um `Usuario` novo ou reaproveita um existente pelo **e-mail**. O ingresso/QR é entregue por **e-mail**. | **Conta com login** (e-mail + senha), área autenticada "meus ingressos" e histórico de pedidos. |
| **RN-5** | Autenticação do administrador | **JWT próprio.** `POST /api/admin/login` (e-mail + senha) retorna um token; todas as rotas `/api/admin/**` exigem o token. Senhas com hash **BCrypt**. Chave/segredo do JWT via variável de ambiente. | Rotação de credenciais, múltiplos perfis de admin, ou login via provedor externo (OAuth2) se necessário. |
| **RN-6** | Geração do código QR do ingresso | O `codigo_qr` é gerado **somente após a confirmação do pagamento**. Na reserva o ingresso fica `RESERVADO` **sem código**; ao aprovar o pagamento, gera-se um identificador único (UUID / hash assinado) e o status passa a `VENDIDO`. | — |

## Fluxo de compra (resumo)

Detalhe completo em [`diagramaSequencia.drawio`](diagramaSequencia.drawio).

1. Cliente escolhe um **lote** de um evento e envia seus dados (RN-4).
2. Sistema resolve/cria o `Usuario` pelo e-mail.
3. Sob **bloqueio de concorrência** (lock pessimista), busca um `Ingresso`
   `DISPONIVEL` do lote. Se não houver, retorna **esgotado** (HTTP 409).
4. Marca o ingresso como `RESERVADO` (sem `codigo_qr` — RN-6) e decrementa
   `quantidade_disponivel` do lote.
5. Cria `Pedido` `PENDENTE` e `Pagamento` `PENDENTE`; `valor_total` = preço do
   lote (RN-1: 1 ingresso).
6. Chama o gateway de pagamento e obtém o link/QR de pagamento (RN-3).
7. Retorna ao cliente os dados de pagamento.

### Confirmação de pagamento (webhook)

- O gateway notifica o sistema em `POST /api/webhooks/pagamento`.
- O processamento é **idempotente** (a notificação pode se repetir).
- **Aprovado:** `Pagamento` → `APROVADO`; `Pedido` → `PAGO`; `Ingresso` →
  `VENDIDO` com `codigo_qr` gerado (RN-6); e-mail enviado ao cliente (RN-4).
- **Recusado / expirado:** libera a reserva do ingresso, repõe o estoque do lote
  e cancela o pagamento.

### Expiração de reservas (job)

- A cada intervalo configurável, o sistema busca `Pedido`s `PENDENTE` criados há
  mais de **15 minutos** (RN-2).
- Para cada um: `Pedido` → `EXPIRADO`; ingresso `RESERVADO` → `DISPONIVEL`;
  `quantidade_disponivel` do lote é reposta; `Pagamento` pendente é cancelado.

## Máquinas de estado

**Ingresso** (`StatusIngresso`)
```
DISPONIVEL --reservar--> RESERVADO --confirmarVenda--> VENDIDO --utilizar--> UTILIZADO
RESERVADO --liberarReserva--> DISPONIVEL      (pagamento recusado ou reserva expirada)
```

**Pedido** (`StatusPedido`)
```
PENDENTE --marcarPago--> PAGO
PENDENTE --cancelar----> CANCELADO
PENDENTE --expirar-----> EXPIRADO            (novo estado — RN-2)
```

**Pagamento** (`StatusPagamento`)
```
PENDENTE --aprovar--> APROVADO
PENDENTE --recusar--> RECUSADO
PENDENTE --cancelar--> CANCELADO
```
Mapeável para os status do Mercado Pago (`approved`, `rejected`, `pending`,
`cancelled`) quando a integração real for implementada.

## Pendências (não bloqueiam o desenvolvimento)

- **Template do e-mail** de envio do ingresso (RN-4). Até a definição, a porta
  `NotificacaoPort` fica como *stub*.
