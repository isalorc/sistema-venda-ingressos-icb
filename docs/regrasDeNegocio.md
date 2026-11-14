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
| **RN-1** | Ingressos por compra | **1 a 5 ingressos por pedido**, todos do mesmo lote, com **um nome de participante por ingresso**. Limite configurável em `application.yml` (`app.compra.max-ingressos-por-pedido`). A reserva é tudo ou nada. | — |
| **RN-2** | Expiração da reserva (TTL) | Reserva não paga expira **15 minutos** após a criação do `Pedido` (status `PENDENTE`). Valor configurável em `application.yml`. Um job periódico libera as reservas expiradas. | — |
| **RN-3** | Métodos de pagamento | **PIX** e **CARTÃO**. Enum `MetodoPagamento = { PIX, CARTAO }`. | Adicionar **BOLETO** (compensação D+1, TTL de reserva próprio). |
| **RN-4** | Identificação do cliente | **Conta obrigatória para comprar** (nome, e-mail, telefone e senha com hash BCrypt). Login devolve um JWT de cliente (`papel=CLIENTE`, 7 dias). A área do cliente lista os pedidos e mostra o QR de cada ingresso pago. Quem comprou sem conta antes disso ativa a conta cadastrando-se com o mesmo e-mail. O ingresso/QR também é entregue por **e-mail** (stub). | Recuperação de senha e confirmação de e-mail (dependem do envio real de e-mail). |
| **RN-5** | Autenticação do administrador | **JWT próprio.** `POST /api/admin/login` (e-mail + senha) retorna um token; todas as rotas `/api/admin/**` exigem o token. Senhas com hash **BCrypt**. **Um único administrador**, provisionado por variável de ambiente (`ADMIN_EMAIL` / `ADMIN_SENHA_HASH`) — sem tela/endpoint de cadastro. Chave/segredo do JWT via variável de ambiente. | Rotação de credenciais, múltiplos perfis de admin (ver "Questões em aberto"), ou login via provedor externo (OAuth2) se necessário. |
| **RN-6** | Geração do código QR do ingresso | O `codigo_qr` é gerado **somente após a confirmação do pagamento**. Na reserva o ingresso fica `RESERVADO` **sem código**; ao aprovar o pagamento, gera-se um identificador único (UUID / hash assinado) e o status passa a `VENDIDO`. | — |

## Fluxo de compra (resumo)

Detalhe completo em [`diagramaSequencia.drawio`](diagramaSequencia.drawio).

1. Cliente **logado** escolhe um **lote** e informa de 1 a 5 nomes de
   participante, um por ingresso (RN-1, RN-4).
2. Sistema identifica o `Usuario` pelo token (papel `CLIENTE`).
3. Sob **bloqueio de concorrência** (lock pessimista no lote), confere se o evento
   e o lote estão ativos e se há ingressos suficientes. Se não houver, retorna
   HTTP 409 com quantos restam e não cria nada.
4. Cria `Pedido` `PENDENTE` com o lote, a quantidade e `valor_total` = preço do
   lote × quantidade (RN-1).
5. Marca cada ingresso como `RESERVADO` com o nome do seu participante (sem
   `codigo_qr` — RN-6) e decrementa `quantidade_disponivel` do lote. É tudo ou
   nada: ou reserva todos, ou nenhum.
6. Cria `Pagamento` `PENDENTE`, chama o gateway e obtém o link/QR de pagamento
   (RN-3).
7. Retorna ao cliente os dados de pagamento; o pedido aparece em "Meus pedidos".

### Confirmação de pagamento (webhook)

- O gateway notifica o sistema em `POST /api/webhooks/pagamento`.
- O processamento é **idempotente** (a notificação pode se repetir).
- **Aprovado:** `Pagamento` → `APROVADO`; `Pedido` → `PAGO`; cada `Ingresso` do
  pedido → `VENDIDO` com o seu próprio `codigo_qr` (RN-6); e-mail enviado ao
  cliente (RN-4).
- **Recusado / expirado:** libera a reserva de todos os ingressos do pedido,
  repõe o estoque do lote e cancela o pagamento.

### Expiração de reservas (job)

- A cada intervalo configurável, o sistema busca `Pedido`s `PENDENTE` criados há
  mais de **15 minutos** (RN-2).
- Para cada um: `Pedido` → `EXPIRADO`; os ingressos `RESERVADO` → `DISPONIVEL`
  (o nome do participante é apagado); `quantidade_disponivel` do lote é reposta;
  `Pagamento` pendente é cancelado.
- Como a varredura roda a cada ~1 minuto, a área do cliente já mostra o pedido
  como **reserva expirada** assim que o prazo passa, sem esperar o job.
- Os horários usam o fuso da igreja (`app.fuso-horario`, America/Sao_Paulo), e
  não o do servidor.

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

## Questões em aberto (pensar bem mais pra frente — pode nem fazer sentido)

- **Múltiplos administradores com papéis (RN-5).** Hoje há um admin único vindo de
  variável de ambiente. *Porém*: talvez faça sentido manter esse admin "raiz"
  provisionado desde o início e permitir que ele **cadastre outros admins com
  menos responsabilidades** (ex.: um perfil que só consulta inscritos, sem criar
  evento/lote). Isso exigiria entidade `Administrador` + papéis + endpoints
  `POST /api/admin/administradores` protegidos pelo próprio JWT de admin raiz.
  Não é para o MVP nem para os próximos épicos — anotado só para não perder a
  ideia. A usuária ainda não decidiu se vale a pena.
