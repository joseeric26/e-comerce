// PARTE 1 — seleção do banco e criação explícita da coleção.
// Em scripts, getSiblingDB equivale ao comando interativo use <banco>.
db = db.getSiblingDB('catalogo_ecommerce_academico');
if (!db.getCollectionNames().includes('produtos')) {
  db.createCollection('produtos');
}

// Não apaga dados nem duplica documentos ao executar novamente.
if (db.produtos.countDocuments({}) === 0) {
  printjson(db.produtos.insertMany([
    {
      _id: 'CAM-001',
      titulo: 'Camiseta básica',
      descricao: 'Camiseta de algodão para uso diário.',
      preco: NumberDecimal('79.90'),
      tags: ['vestuário', 'promoção'],
      fabricante: { nome: 'Moda Solar', pais: 'Brasil' },
      variacoes: [
        { tamanho: 'M', cor: 'Azul', estoque: 12 },
        { tamanho: 'G', cor: 'Preta', estoque: 8 }
      ]
    },
    {
      _id: 'CAM-002',
      titulo: 'Camiseta esportiva',
      descricao: 'Camiseta de algodão para atividades leves.',
      preco: NumberDecimal('129.90'),
      tags: ['vestuário', 'esporte'],
      fabricante: { nome: 'Movimento', pais: 'Brasil' },
      // M e Azul existem, mas em elementos DIFERENTES.
      variacoes: [
        { tamanho: 'M', cor: 'Vermelha', estoque: 6 },
        { tamanho: 'G', cor: 'Azul', estoque: 10 }
      ]
    },
    {
      _id: 'TEN-001',
      titulo: 'Tênis urbano',
      descricao: 'Tênis confortável para caminhadas na cidade.',
      preco: NumberDecimal('249.90'),
      tags: ['calçados', 'promoção'],
      fabricante: { nome: 'Passo Livre', pais: 'Brasil' },
      variacoes: [
        { tamanho: '39', cor: 'Azul', estoque: 4 },
        { tamanho: '40', cor: 'Preta', estoque: 7 }
      ]
    }
  ]));
} else {
  print('Coleção já contém dados: inserção ignorada para preservar os documentos.');
}
print('PARTE 1 — produtos cadastrados:');
db.produtos.find({}).sort({ _id: 1 }).forEach(doc => printjson(doc));
