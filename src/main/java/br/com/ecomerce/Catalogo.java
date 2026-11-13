package br.com.ecomerce;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Updates.set;

/** Modelagem documental baseada nos casos de uso de ecomerce.asta. */
public final class Catalogo {
    private final MongoDatabase db;
    private final MongoCollection<Document> produtos;

    public Catalogo(MongoDatabase db) {
        this.db = db;
        this.produtos = db.getCollection("produtos");
    }

    public void preparar() {
        for (String nome : List.of("produtos", "clientes", "carrinhos", "pedidos")) {
            if (!db.listCollectionNames().into(new ArrayList<>()).contains(nome)) {
                db.createCollection(nome);
            }
        }
        // Parte 1: insertMany, arrays e subdocumentos. Preserva coleções existentes.
        inserirSeVazio(produtos, List.of(
                produto("CAM-001", "Camiseta básica", "Camiseta de algodão para uso diário.",
                        "79.90", List.of("vestuário", "promoção"), "Moda Solar",
                        List.of(variacao("M", "Azul", 12), variacao("G", "Preta", 8))),
                produto("CAM-002", "Camiseta esportiva", "Camiseta de algodão para atividades leves.",
                        "129.90", List.of("vestuário", "esporte"), "Movimento",
                        List.of(variacao("M", "Vermelha", 6), variacao("G", "Azul", 10))),
                produto("TEN-001", "Tênis urbano", "Tênis confortável para caminhadas na cidade.",
                        "249.90", List.of("calçados", "promoção"), "Passo Livre",
                        List.of(variacao("39", "Azul", 4), variacao("40", "Preta", 7)))
        ));
        // Parte 2: índice simples e um único índice de texto em dois campos.
        produtos.createIndex(Indexes.ascending("preco"), new IndexOptions().name("idx_preco"));
        produtos.createIndex(Indexes.compoundIndex(Indexes.text("titulo"), Indexes.text("descricao")),
                new IndexOptions().name("idx_texto").defaultLanguage("portuguese"));
        prepararModeloComplementar();
    }

    public List<Document> todos() {
        return consultar(new Document());
    }

    public List<Document> porPreco() {
        return consultar(filtroPreco());
    }

    public List<Document> emPromocao() {
        return consultar(eq("tags", "promoção"));
    }

    public List<Document> buscaTextual() {
        return consultar(text("algodão"));
    }

    public List<Document> variacaoMAzul() {
        return consultar(elemMatch("variacoes", and(eq("tamanho", "M"), eq("cor", "Azul"))));
    }

    public List<Document> condicoesIndependentes() {
        return consultar(and(eq("variacoes.tamanho", "M"), eq("variacoes.cor", "Azul")));
    }

    public Document explicar(boolean usarIndice) {
        var consulta = produtos.find(filtroPreco());
        if (usarIndice) consulta.hintString("idx_preco");
        return consulta.explain(ExplainVerbosity.EXECUTION_STATS);
    }

    private List<Document> consultar(Bson filtro) {
        return produtos.find(filtro).sort(ascending("_id")).into(new ArrayList<>());
    }

    private Bson filtroPreco() {
        return and(gt("preco", 50), lt("preco", 200));
    }

    private void prepararModeloComplementar() {
        inserirSeVazio(db.getCollection("clientes"), List.of(new Document("_id", "CLI-001")
                .append("nome", "Cliente Exemplo").append("email", "cliente@example.com")));
        inserirSeVazio(db.getCollection("carrinhos"), List.of(new Document("_id", "CAR-001")
                .append("cliente_id", "CLI-001").append("status", "ativo")
                .append("itens", List.of(new Document("produto_id", "CAM-001")
                        .append("tamanho", "M").append("cor", "Azul").append("quantidade", 1)))));
        inserirSeVazio(db.getCollection("pedidos"), List.of(new Document("_id", "PED-001")
                .append("cliente_id", "CLI-001")
                .append("criado_em", Date.from(Instant.parse("2026-11-13T12:00:00Z")))
                .append("status", "aguardando_pagamento")
                .append("itens", List.of(new Document("produto_id", "CAM-001")
                        .append("titulo", "Camiseta básica").append("tamanho", "M")
                        .append("cor", "Azul").append("quantidade", 1)
                        .append("preco_unitario", dinheiro("79.90"))))
                .append("total", dinheiro("79.90"))
                .append("pagamento", new Document("gateway", "gateway_exemplo")
                        .append("status", "pendente").append("transacao_id", null))));
        // Contagem administrativa fixa; não representa reserva ou baixa de venda.
        produtos.updateOne(and(eq("_id", "CAM-001"),
                        elemMatch("variacoes", and(eq("tamanho", "M"), eq("cor", "Azul")))),
                set("variacoes.$[v].estoque", 12),
                new UpdateOptions().arrayFilters(List.of(and(eq("v.tamanho", "M"), eq("v.cor", "Azul")))));
    }

    private void inserirSeVazio(MongoCollection<Document> colecao, List<Document> documentos) {
        if (colecao.countDocuments() == 0) colecao.insertMany(documentos);
    }

    private Document produto(String id, String titulo, String descricao, String preco,
                             List<String> tags, String fabricante, List<Document> variacoes) {
        return new Document("_id", id).append("titulo", titulo).append("descricao", descricao)
                .append("preco", dinheiro(preco)).append("tags", tags)
                .append("fabricante", new Document("nome", fabricante).append("pais", "Brasil"))
                .append("variacoes", variacoes);
    }

    private Document variacao(String tamanho, String cor, int estoque) {
        return new Document("tamanho", tamanho).append("cor", cor).append("estoque", estoque);
    }

    private Decimal128 dinheiro(String valor) {
        return new Decimal128(new BigDecimal(valor));
    }
}
