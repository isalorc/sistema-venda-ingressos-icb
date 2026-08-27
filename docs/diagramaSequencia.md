# Fluxo de Compra de Ingresso (Arquitetura Hexagonal)

O diagrama abaixo detalha o fluxo sequencial desde a requisição do frontend até as integrações externas, passando pelas portas e adaptadores.

```mermaid
sequenceDiagram
    autonumber
    actor Fiel as Angular (Cliente)
    participant Ctrl as IngressoController<br/>(Adapter In)
    participant UC as ComprarIngressoUseCase<br/>(Core/Domain)
    participant Repo as IngressoRepositoryPort<br/>(Adapter Out - BD)
    participant PG as GatewayPagamentoPort<br/>(Adapter Out - API)

    Fiel->>Ctrl: POST /api/ingressos/comprar {idEvento}
    Ctrl->>UC: comprar(idEvento, idUsuario)
    
    rect rgb(240, 248, 255)
        Note right of UC: Bloqueio de Concorrência (Pessimistic Lock)
        UC->>Repo: buscarIngressoDisponivelComBloqueio(idEvento)
        Repo-->>UC: Retorna Ingresso (Status: DISPONÍVEL)
    end
    
    alt Ingressos Esgotados
        UC-->>Ctrl: throw IngressoEsgotadoException
        Ctrl-->>Fiel: 400 Bad Request (Esgotado)
    else Ingresso Encontrado
        UC->>UC: altera status para RESERVADO
        UC->>Repo: salvar(ingresso)
        Repo-->>UC: Ingresso atualizado
        
        Note right of UC: Integração com Mercado Pago
        UC->>PG: gerarLinkDePagamento(ingresso, idUsuario)
        PG-->>UC: Retorna QRCode / Link PIX
        
        UC-->>Ctrl: ReservaIngressoDto
        Ctrl-->>Fiel: 200 OK (Com QRCode PIX)
    end