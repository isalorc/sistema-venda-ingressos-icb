# Sistema de venda de ingressos ICB

Projeto para gerenciar a venda de ingressos da ICB, estruturado em módulos de domínio, casos de uso, portas e adapters.

## Estrutura inicial

- app/
  - domain: regras de negócio centrais
  - usecase: casos de uso da aplicação
  - ports: interfaces de saída/entrada
  - adapter: implementação de infraestrutura e integrações

## Como iniciar

1. Acesse a pasta do projeto:
   ```bash
   cd app
   ```
2. Compile e teste os módulos:
   ```bash
   mvn clean install
   ```

## Tecnologias

- Java 17
- Maven
- JUnit 5
