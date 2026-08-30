# Collection Postman — sistema-venda-ingressos-icb

Arquivos:

| Arquivo | Conteúdo |
|---|---|
| `sistema-venda-ingressos-icb.postman_collection.json` | Todas as requests da API, com scripts de teste e um exemplo de response salvo por request. |
| `sistema-venda-ingressos-icb.dev.postman_environment.json` | Environment do perfil `dev` (variáveis e credenciais). |

## Como usar

1. Suba a API no perfil `dev`:

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. No Postman: **Import** os dois arquivos acima.
3. Selecione o environment **sistema-venda-ingressos-icb — dev** (canto superior direito).
4. Abra a pasta **1 · Fluxo de compra (feliz, PIX)** e rode as requests **de cima
   para baixo** — a 1ª (**Admin · Login**) guarda o token em `adminToken`; as
   demais gravam no environment o que a próxima precisa (`eventoId`, `loteId`,
   `pedidoId`, `referenciaGateway`). Dá para rodar a pasta inteira de uma vez
   pelo **Collection Runner**.

Para os cenários de webhook (recusado / sem token / status intermediário), rode
antes a compra da pasta **2 · Webhook — cenários**, que gera um novo pagamento
`PENDENTE`.

## Autenticação do admin (Épico 8)

As rotas `/api/admin/**` (exceto `/api/admin/login`) exigem
`Authorization: Bearer <token>`. A request **Admin · Login** (pasta 1) faz o
login com `{{adminEmail}}` / `{{adminSenha}}` e salva o token em `adminToken`; as
requests admin já mandam o header `Bearer {{adminToken}}`. O token **expira em
2h** (`app.jwt.expiracao`) — rode o Login de novo se receber 401.

## Variáveis do environment

| Variável | Uso |
|---|---|
| `baseUrl` | `http://localhost:8080` |
| `webhookSecret` | valor do header `X-Webhook-Token` (= `app.webhook.secret`, `dev-webhook-secret` no perfil dev) |
| `adminEmail` / `adminSenha` | credencial de dev do admin (`admin@icb.local` / `admin123`) |
| `adminToken` | JWT preenchido pela request **Admin · Login** |
| `dataHoraEvento` | recalculada no *pre-request* do cadastro de evento para hoje + 90 dias (o cadastro exige data futura) |
| `eventoId`, `loteId`, `pedidoId`, `referenciaGateway`, `qrCodePix` | preenchidas pelos scripts de teste durante o fluxo |

## Observações

- **Persistência em memória:** os dados somem quando a API reinicia — recomece
  pela pasta 1.
- **Gateway fake:** a `referenciaGateway` usada no webhook é extraída da
  `urlPagamento` retornada na compra (último segmento, `fake-<uuid>`).
- **Contrato navegável:** `http://localhost:8080/swagger-ui.html`.
- Cobre os Épicos 4 a 8: consulta, compra, webhook (RF-16), scheduler e o login
  admin com JWT (RF-19 / RF-26).
