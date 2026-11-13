package br.com.ecomerce;

import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
class CatalogoTest {
    @Test
    void modelagemConsultasIndicesEReexecucao() {
        try (var client = MongoClients.create(System.getenv("MONGODB_URI"))) {
            // Banco exclusivo do teste; nenhum dado do catálogo real é apagado.
            var db = client.getDatabase("teste_catalogo_" + UUID.randomUUID().toString().replace("-", ""));
            try {
                var catalogo = new Catalogo(db);
                for (int vez = 0; vez < 2; vez++) {
                    catalogo.preparar();
                    var produtos = db.getCollection("produtos");
                    assertEquals(3, produtos.countDocuments());
                    for (var doc : produtos.find()) {
                        assertFalse(doc.getList("tags", String.class).isEmpty());
                        assertNotNull(doc.get("fabricante", Document.class).getString("nome"));
                        assertFalse(doc.getList("variacoes", Document.class).isEmpty());
                    }
                    assertEquals(List.of("CAM-001", "CAM-002"), ids(catalogo.porPreco()));
                    assertEquals(List.of("CAM-001", "TEN-001"), ids(catalogo.emPromocao()));
                    assertEquals(List.of("CAM-001", "CAM-002"), ids(catalogo.buscaTextual()));
                    assertEquals(List.of("CAM-001"), ids(catalogo.variacaoMAzul()));
                    assertEquals(List.of("CAM-001", "CAM-002"), ids(catalogo.condicoesIndependentes()));
                    var indices = produtos.listIndexes().into(new java.util.ArrayList<>());
                    assertTrue(indices.stream().anyMatch(i -> "idx_preco".equals(i.getString("name"))));
                    assertEquals(1, indices.stream().filter(i -> i.containsKey("textIndexVersion")).count());
                    var plano = catalogo.explicar(true);
                    var stats = plano.get("executionStats", Document.class);
                    assertEquals(2, ((Number) stats.get("nReturned")).intValue());
                    assertTrue(((Number) stats.get("totalKeysExamined")).longValue() > 0);
                    assertTrue(plano.get("queryPlanner", Document.class).toJson().contains("IXSCAN"));
                    assertEquals(1, db.getCollection("clientes").countDocuments());
                    assertEquals(1, db.getCollection("carrinhos").countDocuments());
                    assertEquals("CAM-001", db.getCollection("carrinhos").find().first()
                            .getList("itens", Document.class).get(0).getString("produto_id"));
                    assertEquals(1, db.getCollection("pedidos").countDocuments());
                    assertEquals("pendente", db.getCollection("pedidos").find().first()
                            .get("pagamento", Document.class).getString("status"));
                }
            } finally {
                db.drop();
            }
        }
    }

    private List<String> ids(List<Document> documentos) {
        return documentos.stream().map(d -> d.getString("_id")).toList();
    }
}
