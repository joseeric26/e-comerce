# Plataforma de Ecommerce — modelagem no MongoDB

Projeto acadêmico de banco NoSQL orientado a documentos, baseado no diagrama de casos de uso **ecomerce.asta**.

**Link para a entrega:** https://github.com/joseeric26/e-comerce

## Relação com o projeto original

O arquivo original está em [`modelo/ecomerce.asta`](modelo/ecomerce.asta) e pode ser aberto no Astah. Ele contém um diagrama de casos de uso da “Plataforma de Ecommerce.”, com os atores **cliente**, **Administrador** e **Gateway de Pagamento**. Os nomes identificados no arquivo são: **Buscar Produtos**, **Gerenciar Carrinho**, **Realizar Checkout**, **Processar Pagamento** e **Atualizar Estoque**.

Como esse é um diagrama de casos de uso, os campos e as coleções abaixo são uma proposta de modelagem documental para apoiar essas funcionalidades; não são atributos extraídos de um diagrama de classes.

| Caso de uso original | Aplicação na modelagem |
| --- | --- |
| Buscar Produtos | Coleção `produtos`, filtro por preço, tags e busca textual |
| Gerenciar Carrinho | Coleção `carrinhos`, com itens incorporados e referências aos produtos |
| Realizar Checkout | Coleção `pedidos`, com snapshot dos itens e preço na data da compra |
| Processar Pagamento | Subdocumento `pagamento` no pedido, com gateway, status e ID da transação |
| Atualizar Estoque | Estoque por variação do produto e exemplo de `updateOne` com `arrayFilters` |

O foco da atividade é o catálogo. O script 05 complementa o modelo com documentos fictícios de carrinho e pedido. Ele não implementa checkout, reserva de estoque ou comunicação real com um gateway. O carrinho e o pedido são exemplos independentes; não representam uma compra executada pelo script.

## Como executar

Pré-requisitos: MongoDB Community em execução local e `mongosh` instalado. Também é possível usar uma conexão MongoDB Atlas com permissão para criar coleções e índices.

```bash
git clone https://github.com/joseeric26/e-comerce.git
cd e-comerce
mongosh "mongodb://127.0.0.1:27017" --quiet --file executar.js
```

Execute a partir da raiz do repositório, pois `load()` usa caminhos relativos. Não é necessário instalar pacotes npm. Em uma conexão remota, substitua a URI pela sua conexão e mantenha as credenciais fora do repositório.

Alternativamente, no `mongosh`, aberto na raiz do projeto:

```javascript
load('executar.js');
```

O script seleciona o banco `catalogo_ecommerce_academico`. Em arquivos JavaScript, `db = db.getSiblingDB(...)` faz a seleção equivalente a `use catalogo_ecommerce_academico` no shell interativo; o banco é materializado na primeira gravação.

É possível executar cada arquivo separadamente com `mongosh --file scripts/01_dados.js`, seguindo a ordem numérica dos scripts. Todos selecionam o mesmo banco.

**Reexecução:** nenhum banco ou coleção é apagado. Cada inserção ocorre somente se a respectiva coleção estiver vazia; caso contrário, os dados existentes são preservados. Índices iguais podem ser criados novamente. O script 05 fixa o estoque M/Azul de `CAM-001` em 12 unidades como exemplo administrativo. Os resultados abaixo pressupõem o conjunto original, em um banco novo ou previamente criado por estes scripts sem alterações manuais. Coleções parcialmente preenchidas não são completadas automaticamente.

## Arquivos e requisitos

| Parte | Arquivo | Evidência |
| --- | --- | --- |
| 1 | `scripts/01_dados.js` | Criação explícita da coleção e `insertMany()` com 3 produtos |
| 2 | `scripts/02_indices_consultas.js` | Índice simples, índice de texto e 3 consultas |
| 3 | `scripts/03_elemMatch.js` | Filtro de tamanho M e cor Azul na mesma variação |
| Bônus | `scripts/04_explain.js` | `explain('executionStats')`, plano e métricas |
| Modelo original | `scripts/05_modelo_ecomerce.js` | Clientes, carrinhos, pedidos, pagamento e atualização de estoque |
| 4 | Este README | Reflexão sobre embedding e referencing |

Todos os produtos têm `tags` (array de strings), `fabricante` (subdocumento) e `variacoes` (array de subdocumentos). `preco` usa BSON Decimal128 com `NumberDecimal()` para representar valores monetários decimais.

## Parte 2 — índices e consultas

O índice simples `idx_preco` atende à consulta por faixa de preço. O único índice de texto, `idx_texto`, combina `titulo` e `descricao` e usa o idioma português.

| Consulta | Filtro | IDs esperados |
| --- | --- | --- |
| Comparação | `preco: { $gt: 50, $lt: 200 }` | `CAM-001`, `CAM-002` |
| Valor em array | `tags: 'promoção'` | `CAM-001`, `TEN-001` |
| Busca textual | `$text: { $search: 'algodão' }` | `CAM-001`, `CAM-002` |
| Desafio | `variacoes: { $elemMatch: { tamanho: 'M', cor: 'Azul' } }` | `CAM-001` |

A busca `$text` trabalha com termos e regras linguísticas; não equivale a uma busca por qualquer trecho da palavra. Para busca parcial sem diferenciar maiúsculas de minúsculas, pode-se usar `db.produtos.find({ titulo: { $regex: 'camis', $options: 'i' } })`, mas essa consulta não usa o índice de texto. Filtros de igualdade comuns, na configuração padrão deste projeto, diferenciam maiúsculas de minúsculas.

## Parte 3 — por que usar `$elemMatch`?

No produto `CAM-002`, existe uma variação M/Vermelha e outra G/Azul. Condições separadas em `variacoes.tamanho` e `variacoes.cor` podem retornar esse produto, mesmo sem existir M/Azul. O `$elemMatch` exige que ambas as condições sejam satisfeitas pelo **mesmo elemento** do array. Por isso, somente `CAM-001` atende ao desafio. O script mostra as duas consultas para comparação. [Documentação do operador](https://www.mongodb.com/docs/manual/reference/operator/query/elemmatch/).

### Bônus — comprovação do índice

O script 04 executa primeiro a consulta sem `hint()` e depois com `.hint('idx_preco')`. Na execução com o índice, confira:

- `winningPlan`: estágio `IXSCAN` e nome `idx_preco` (podem estar dentro de estágios aninhados).
- `executionStats.totalKeysExamined`: maior que zero, indicando chaves de índice examinadas.
- `executionStats.nReturned`: 2 no conjunto original.
- `executionStats.totalDocsExamined`: documentos examinados para atender à consulta.

Com apenas três produtos, o otimizador pode preferir `COLLSCAN` sem `hint()`. O `hint()` força o índice para a demonstração, sem comprovar que ele seria escolhido espontaneamente ou que seria mais rápido nessa base pequena. Os números reais e o plano completo são impressos durante a execução. [Como interpretar o explain](https://www.mongodb.com/docs/manual/reference/explain-results/).

## Parte 4 — reflexão: Embedding e Referencing

**A principal diferença de arquitetura é onde os dados relacionados ficam armazenados.** No **embedding**, os dados ficam dentro do mesmo documento, como um subdocumento ou array. No **referencing**, ficam em documentos separados, ligados por um identificador; para recuperar o conjunto, a aplicação realiza consultas adicionais ou uma agregação com `$lookup`.

Embedding é ideal para dados lidos juntos, com tamanho controlado e pouca alteração independente. No catálogo, as variações são lidas junto com o produto; no pedido, os itens e os preços ficam incorporados para preservar o histórico da compra. Dados que mudam juntos também podem ser incorporados: operações sobre um único documento são atômicas. A frequência de atualização, sozinha, não determina a escolha.

Referencing é adequado quando entidades são compartilhadas, mudam frequentemente de forma independente ou têm relacionamentos muitos-para-muitos. Neste projeto, `cliente_id` referencia `clientes` e `produto_id` referencia `produtos`, evitando copiar o cadastro completo em cada carrinho. Categorias compartilhadas por vários produtos também poderiam usar referências. Esses IDs são referências manuais: o MongoDB não cria integridade referencial automática como uma chave estrangeira relacional.

O subdocumento `fabricante` foi incorporado por conter apenas um resumo pequeno e estável usado na leitura do catálogo. Se o cadastro do fabricante fosse compartilhado por muitos produtos e sofresse atualizações frequentes, seria melhor mantê-lo em outra coleção e usar `fabricante_id`.

No pedido, a cópia de título e preço é intencional: uma alteração posterior no catálogo não deve reescrever o valor de uma compra antiga. Em um blog, um resumo estável do autor pode ser incorporado; o cadastro atualizado do autor e comentários sem limite de crescimento devem ficar em coleções separadas, com referências.

Arrays precisam ter crescimento controlado: o limite BSON é de 16 MiB por documento. Neste exemplo, as variações e os itens são pequenos; em uma aplicação real, seriam definidos limites para itens e variações. Históricos de pagamentos, comentários e eventos não devem crescer indefinidamente no mesmo documento. [Boas práticas de modelagem do MongoDB](https://www.mongodb.com/docs/manual/data-modeling/best-practices/).

## Entrega

### Verificação realizada

Os scripts foram executados duas vezes no `mongosh` 2.14.2 contra uma instância temporária real do MongoDB 8.2.6. Foram verificados os três produtos, os resultados esperados dos filtros, a diferença entre condições independentes e `$elemMatch`, os índices, as referências dos documentos complementares e `totalKeysExamined > 0` com `IXSCAN` no bônus.

Na máquina de validação, o hostname do Windows contém acento e o servidor retornou esse campo de `serverInfo.host` em uma codificação inválida no `explain`. Somente a conexão de teste foi configurada para tolerar esse retorno; os scripts entregues mantêm os comandos padrão do `mongosh`. Se o mesmo erro `Invalid UTF-8 string in BSON document` ocorrer no bônus em um servidor Windows com hostname acentuado, execute os scripts em outro servidor MongoDB com hostname ASCII, como uma instância remota. As demais consultas foram executadas também com a validação de UTF-8 padrão.

### Link para o professor

Os arquivos `.js` são executáveis diretamente no `mongosh`. O `.asta` acompanha a entrega como referência do projeto original, sem substituir os scripts exigidos. Link a informar ao professor:

**https://github.com/joseeric26/e-comerce**
