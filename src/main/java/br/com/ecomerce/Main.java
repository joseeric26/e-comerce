package br.com.ecomerce;

import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        String uri = ambiente("MONGODB_URI", "mongodb://127.0.0.1:27017");
        String banco = ambiente("MONGODB_DATABASE", "catalogo_ecommerce_academico");
        try (var client = MongoClients.create(uri)) {
            var db = client.getDatabase(banco);
            db.runCommand(new Document("ping", 1));
            var catalogo = new Catalogo(db);
            catalogo.preparar();
            mostrar("PARTE 1 — produtos", catalogo.todos());
            mostrar("PARTE 2 — preço maior que 50 e menor que 200", catalogo.porPreco());
            mostrar("PARTE 2 — tag promoção", catalogo.emPromocao());
            mostrar("PARTE 2 — busca textual por algodão", catalogo.buscaTextual());
            mostrar("PARTE 3 — mesma variação M/Azul ($elemMatch)", catalogo.variacaoMAzul());
            mostrar("Comparação — condições independentes", catalogo.condicoesIndependentes());
            System.out.println("BÔNUS — plano sem hint:");
            System.out.println(catalogo.explicar(false).toJson());
            System.out.println("BÔNUS — plano com hint idx_preco:");
            Document plano = catalogo.explicar(true);
            System.out.println(plano.toJson());
            System.out.println("Métricas: " + plano.get("executionStats", Document.class).toJson());
            for (String colecao : List.of("clientes", "carrinhos", "pedidos")) {
                mostrar("MODELO ORIGINAL — " + colecao, db.getCollection(colecao).find()
                        .into(new java.util.ArrayList<>()));
            }
            System.out.println("Estoque administrativo M/Azul de CAM-001 definido em 12 unidades.");
        } catch (RuntimeException e) {
            // Não imprime a URI, que pode conter credenciais.
            System.err.println("Não foi possível concluir a execução (" + e.getClass().getSimpleName()
                    + "). Verifique conexão, permissões e compatibilidade dos dados/índices no banco.");
            System.exit(1);
        }
    }

    private static String ambiente(String chave, String padrao) {
        String valor = System.getenv(chave);
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    private static void mostrar(String titulo, List<Document> documentos) {
        System.out.println(titulo);
        documentos.forEach(doc -> System.out.println(doc.toJson()));
    }
}
