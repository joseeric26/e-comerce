db = db.getSiblingDB('catalogo_ecommerce_academico');

// PARTE 2 — um índice simples e um único índice de texto em dois campos.
printjson(db.produtos.createIndex({ preco: 1 }, { name: 'idx_preco' }));
printjson(db.produtos.createIndex(
  { titulo: 'text', descricao: 'text' },
  { name: 'idx_texto', default_language: 'portuguese' }
));

print('CONSULTA 1 — preço maior que R$ 50 e menor que R$ 200:');
// Esperado: CAM-001 e CAM-002.
db.produtos.find({ preco: { $gt: 50, $lt: 200 } })
  .sort({ _id: 1 }).forEach(doc => printjson(doc));

print('CONSULTA 2 — valor dentro do array tags:');
// Esperado: CAM-001 e TEN-001.
db.produtos.find({ tags: 'promoção' })
  .sort({ _id: 1 }).forEach(doc => printjson(doc));

print('CONSULTA 3 — busca textual por algodão:');
// Esperado: CAM-001 e CAM-002. $text utiliza o índice de texto.
db.produtos.find({ $text: { $search: 'algodão' } })
  .sort({ _id: 1 }).forEach(doc => printjson(doc));
