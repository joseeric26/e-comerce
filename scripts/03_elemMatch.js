db = db.getSiblingDB('catalogo_ecommerce_academico');

print('PARTE 3 — variação com tamanho M E cor Azul no mesmo elemento:');
// Esperado: apenas CAM-001.
db.produtos.find({
  variacoes: { $elemMatch: { tamanho: 'M', cor: 'Azul' } }
}).forEach(doc => printjson(doc));

print('Comparação didática — condições independentes sem $elemMatch:');
// Retorna CAM-001 e CAM-002: M pode estar em uma variação e Azul em outra.
db.produtos.find({
  'variacoes.tamanho': 'M',
  'variacoes.cor': 'Azul'
}).sort({ _id: 1 }).forEach(doc => printjson(doc));
