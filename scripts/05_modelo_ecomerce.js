// Complemento da atividade: modelagem dos casos de uso de ecomerce.asta.
// Dados fictícios; este script não executa um checkout nem chama um gateway.
db = db.getSiblingDB('catalogo_ecommerce_academico');
for (const nome of ['clientes', 'carrinhos', 'pedidos']) {
  if (!db.getCollectionNames().includes(nome)) db.createCollection(nome);
}

if (db.clientes.countDocuments({}) === 0) {
  db.clientes.insertMany([
    { _id: 'CLI-001', nome: 'Cliente Exemplo', email: 'cliente@example.com' }
  ]);
}
if (db.carrinhos.countDocuments({}) === 0) {
  db.carrinhos.insertMany([
    {
      _id: 'CAR-001', cliente_id: 'CLI-001', status: 'ativo',
      itens: [{ produto_id: 'CAM-001', tamanho: 'M', cor: 'Azul', quantidade: 1 }]
    }
  ]);
}
if (db.pedidos.countDocuments({}) === 0) {
  db.pedidos.insertMany([
    {
      _id: 'PED-001', cliente_id: 'CLI-001',
      criado_em: ISODate('2026-11-13T12:00:00Z'),
      status: 'aguardando_pagamento',
      // Snapshot: título e preço representam o momento do pedido.
      itens: [{
        produto_id: 'CAM-001', titulo: 'Camiseta básica',
        tamanho: 'M', cor: 'Azul', quantidade: 1,
        preco_unitario: NumberDecimal('79.90')
      }],
      total: NumberDecimal('79.90'),
      pagamento: { gateway: 'gateway_exemplo', status: 'pendente', transacao_id: null }
    }
  ]);
}

print('MODELO ORIGINAL — Gerenciar Carrinho:');
db.carrinhos.find({ cliente_id: 'CLI-001', status: 'ativo' })
  .forEach(doc => printjson(doc));
print('MODELO ORIGINAL — Realizar Checkout / Processar Pagamento: pedido fictício pendente');
db.pedidos.find({ _id: 'PED-001' }).forEach(doc => printjson(doc));

// Atualização administrativa idempotente por contagem física de estoque.
// Não representa baixa de venda ou reserva de estoque.
print('MODELO ORIGINAL — Atualizar Estoque da variação M/Azul para 12 unidades:');
printjson(db.produtos.updateOne(
  { _id: 'CAM-001', variacoes: { $elemMatch: { tamanho: 'M', cor: 'Azul' } } },
  { $set: { 'variacoes.$[v].estoque': 12 } },
  { arrayFilters: [{ 'v.tamanho': 'M', 'v.cor': 'Azul' }] }
));
