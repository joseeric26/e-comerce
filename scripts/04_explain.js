db = db.getSiblingDB('catalogo_ecommerce_academico');

print('BÔNUS — plano escolhido pelo otimizador, sem hint:');
printjson(db.produtos.find({ preco: { $gt: 50, $lt: 200 } })
  .explain('executionStats'));

// Uma coleção pequena pode favorecer COLLSCAN. Para demonstrar o índice
// de forma reproduzível, hint exige o índice idx_preco criado na Parte 2.
print('BÔNUS — execução usando explicitamente idx_preco:');
const planoComIndice = db.produtos.find({ preco: { $gt: 50, $lt: 200 } })
  .hint('idx_preco').explain('executionStats');
printjson(planoComIndice);
print('Métricas da execução com índice:');
printjson({
  nReturned: planoComIndice.executionStats.nReturned,
  totalKeysExamined: planoComIndice.executionStats.totalKeysExamined,
  totalDocsExamined: planoComIndice.executionStats.totalDocsExamined
});
// Verifique IXSCAN e indexName: idx_preco no winningPlan,
// além de totalKeysExamined > 0. As métricas reais aparecem acima.
