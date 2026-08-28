Projeto: sistema-venda-ingressos-icb
Resumo consolidado da documentação disponível em /docs

1. Visão geral
O projeto é um sistema de venda de ingressos pensado para uma igreja específica (escopo limitado: sem múltiplos locais nem assentos por enquanto). Implementação em Java (Maven, Java 17). A arquitetura proposta é Hexagonal (Ports & Adapters). É o TCC do curso de Análise e Desenvolvimento de Sistemas da Fatec, desenvolvido de forma incremental (MVP micro primeiro, depois evolução).

1.1. Decisões técnicas
- Stack backend: Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security). O core do hexágono (domain, usecase) permanece livre de anotações de framework.
- Frontend: projeto/repositório separado. Este repositório entrega apenas a API REST + webhook de pagamento.
- Gateway de pagamento: porta de saída com adapter fake no MVP; integração real Mercado Pago (sandbox) em etapa posterior.
- Persistência: adapter em memória no MVP para validar casos de uso; depois JPA/PostgreSQL + Flyway + lock pessimista.

1.2. Regras de negócio
As regras de negócio acordadas com o time estão em docs/regrasDeNegocio.md (RN-1 a RN-6): 1 ingresso por pedido, TTL de reserva de 15 min, pagamento PIX/cartão, identificação do fiel sem cadastro, autenticação admin via JWT e geração do QR somente após o pagamento aprovado.

1.3. Requisitos
A análise de requisitos funcionais (RF-01 a RF-27) e não funcionais (RNF-01 a RNF-36), com restrições, premissas e itens fora de escopo, está em docs/analiseDeRequisitos.md.

2. Regras arquiteturais (do agente/IA)
- Single POM: tudo gerenciado no pom.xml raiz; não usar multi-module.
- Estrutura de pacotes esperada: br.com.icb.ingressos.{domain,ports,usecase,adapter/in,adapter/out}.
- domain: entidades puras, sem dependência de frameworks (sem anotações JPA etc.).
- ports: apenas interfaces (contratos).
- usecase: orquestração da lógica de negócio e implementação das portas de entrada.
- adapter: subdividido em adapter.in (controllers/endpoints) e adapter.out (repositórios, clientes externos).
- Regra de dependência: adapters conhecem usecase e domain; usecase/domain não importam adapter.
- Princípios de qualidade: SOLID, imutabilidade, uso de record para DTOs e Java moderno (var quando apropriado).
- Comportamento da IA: perguntar quando houver ambiguidade; não inventar regras financeiras nem campos sem confirmação.

3. Diagrama de sequência (fluxo de compra)
Arquivo: docs/diagramaSequencia.md
Resumo: fluxo desde o cliente -> IngressoController (adapter/in) -> ComprarIngressoUseCase (core) -> IngressoRepositoryPort (adapter/out) -> GatewayPagamentoPort. Inclui bloqueio de concorrência (pessimistic lock), reserva do ingresso, integração com pagamento (gerar link/QR), retorno ao cliente.

4. Modelo de dados (ER simplificado)
Arquivo: docs/modelagemEntidadeRelacionamento.md
Entidades principais: USUARIO, EVENTO, LOTE, INGRESSO, PEDIDO, PAGAMENTO.
Relações-chave: USUARIO realiza PEDIDO; PEDIDO contém INGRESSOS e tem PAGAMENTO(s); EVENTO possui LOTEs; LOTE gera INGRESSOS.
Observação: LOCAL e ASSENTO foram removidos (escopo igreja única simplificado).

5. Estado atual do código (artefatos criados)
- src/main/java/br/com/icb/ingressos/application/SistemaVendaIngressosApplication.java (main) — chama o starter (atualmente existe ApplicationStarter em mesma package).
- src/main/java/br/com/icb/ingressos/application/ApplicationStarter.java (starter simples que carrega configuração e chama Initializer).
- src/main/java/br/com/icb/ingressos/application/Initializer.java (placeholder de inicialização).
- Nota: Também existem arquivos iniciais criados em br.com.icb.application (Main e Initializer) — possivelmente redundantes.

6. Como compilar/executar rapidamente
- mvn compile
- mvn exec:java -Dexec.mainClass=br.com.icb.ingressos.application.SistemaVendaIngressosApplication

7. Próximos passos recomendados
O backlog técnico completo (Épicos 0 a 11, sequência de execução e marco de MVP) está no plano de desenvolvimento. Resumo:
- Épico 0: configurar Spring Boot no pom.xml, converter o main em @SpringBootApplication, application.yml (perfis dev/prod), tratamento global de erros.
- Épico 1: transformar as entidades anêmicas em modelo rico (transições de estado, invariantes de estoque, exceções de domínio) + testes.
- Épico 2: portas de entrada (ports.in) e de saída (ports.out), incluindo GatewayPagamentoPort e busca com bloqueio.
- Épico 3: casos de uso — núcleo em ComprarIngressoService, ConfirmarPagamentoService (idempotente) e ExpirarReservasService.
- Épicos 4-7: adapter REST, persistência em memória, gateway fake, scheduler de reservas -> MVP navegável.
- Épicos 8-11: segurança JWT, PostgreSQL + Flyway, qualidade/CI (ArchUnit, Jacoco, Testcontainers), Mercado Pago real, deploy.
