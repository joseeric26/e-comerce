# Conferência dos requisitos da atividade

Conferência realizada em 13/11/2026. Repositório de entrega: https://github.com/joseeric26/e-comerce

| Requisito | Situação | Evidência |
| --- | --- | --- |
| Catálogo de e-commerce em MongoDB | Atendido | Coleção `produtos`, baseada nos casos de uso de `modelo/ecomerce.asta` |
| Criação do banco e da coleção principal | Atendido | `scripts/01_dados.js`: `getSiblingDB()` e `createCollection()`; Java: seleção no `Main` e criação em `Catalogo.preparar()` |
| No mínimo 3 documentos com `insertMany()` | Atendido | `CAM-001`, `CAM-002` e `TEN-001`, no script 01 e no Java |
| Array em cada documento | Atendido | `tags` e `variacoes` nos três produtos |
| Subdocumento em cada documento | Atendido | `fabricante` nos três produtos |
| Índice simples em campo de consulta | Atendido | `idx_preco`, com `{ preco: 1 }`, script 02 e Java |
| Índice de texto | Atendido | Um único `idx_texto` em `titulo` e `descricao`, script 02 e Java |
| Consulta com operadores de comparação | Atendido | Preço com `$gt: 50` e `$lt: 200`, retornando `CAM-001` e `CAM-002` |
| Consulta de valor dentro de array | Atendido | Tag `promoção`, retornando `CAM-001` e `TEN-001` |
| Consulta usando índice de texto | Atendido | `$text` por `algodão`, retornando `CAM-001` e `CAM-002` |
| Consulta `$elemMatch` em array de subdocumentos | Atendido | M e Azul na mesma variação, retornando somente `CAM-001` |
| Bônus: `explain("executionStats")` | Atendido | Script 04 e `Catalogo.explicar()`, com demonstração de `IXSCAN` e `totalKeysExamined: 2` |
| Reflexão no README | Atendido | Seção “Parte 4”: diferença arquitetural, cenários de uso, dados lidos juntos, atualizações e muitos-para-muitos |
| Scripts executáveis no `mongosh` | Atendido | `executar.js` carrega os scripts numerados; permanecem na entrega junto ao Java |
| Repositório GitHub e link para entrega | Atendido | Link no README e neste arquivo |

## Validação executada

- Java: `mvn package` com MongoDB real 8.2.6; **BUILD SUCCESS**.
- JUnit: **1 teste de integração, 0 falhas, 0 erros, 0 ignorados**. O teste exercita duas preparações e compara os IDs das consultas.
- JAR: duas execuções completas no mesmo banco temporário; três produtos ao final, sem duplicação.
- Bônus: plano `IXSCAN`, índice `idx_preco`, `nReturned: 2`, `totalKeysExamined: 2`, `totalDocsExamined: 2`.
- Scripts mongosh: duas execuções completas com dados, filtros e índices verificados. O ajuste de conexão necessário por causa do hostname acentuado da máquina de teste está descrito no README.

## Observações para a entrega

A implementação principal agora é Java, conforme solicitado. Os `.js` foram mantidos porque o enunciado exige scripts que o professor possa executar diretamente no `mongosh`; entregar somente arquivos Java não atenderia literalmente a essa orientação.

O uso de `hint('idx_preco')` no bônus força o índice para demonstrar seu funcionamento. Também é mostrado o plano sem `hint`. Isso não comprova ganho de velocidade em uma coleção com três documentos; comprova a execução usando o índice.

Carrinho, pedido e pagamento são documentos fictícios complementares ao catálogo. Não há processamento real de compra ou pagamento, pois isso não é exigido pela atividade de modelagem NoSQL.
