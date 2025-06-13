package compass.javaapirecommendation.service;

import compass.javaapirecommendation.client.PythonEmbeddingsClient;
import org.springframework.stereotype.Service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Serviço responsável pela lógica de recomendação de meditações.
 * Inclui chamadas para a API Python para vetorização e busca no MongoDB.
 */
@Service
public class RecommendationService {

    // Logger para registrar informações e erros
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    // Cliente Feign para a API Python
    private final PythonEmbeddingsClient pythonEmbeddingsClient;

    // Cliente MongoDB
    private final MongoClient mongoClient;

    public RecommendationService(PythonEmbeddingsClient pythonEmbeddingsClient, MongoClient mongoClient) {
        this.pythonEmbeddingsClient = pythonEmbeddingsClient;
        this.mongoClient = mongoClient;
    }


    /**
     * Gera um embedding para o texto fornecido chamando a API Python via Feign.
     * @param text O texto a ser vetorizado.
     * @return Uma lista de Double representando o embedding.
     * @throws RuntimeException se a chamada à API Python falhar.
     */
    public List<Double> generateEmbedding(String text) {
        logger.info("Chamando API Python (via Feign) para gerar embedding para: {}", text);
        try {
            // Cria um corpo de requisição JSON com o texto para o cliente Feign
            Map<String, String> requestBody = Collections.singletonMap("text", text);

            // Faz a chamada POST para a API Python usando o cliente Feign
            List<Double> embedding = pythonEmbeddingsClient.getEmbedding(requestBody);

            logger.info("Embedding gerado com sucesso via Feign.");
            return embedding;
        } catch (Exception e) {
            logger.error("Erro inesperado ao chamar API Python (via Feign) para embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Realiza a busca vetorial no MongoDB usando o embedding fornecido.
     * Assume que os embeddings das meditações estão armazenados em uma coleção.
     * @param embedding O vetor (embedding) para busca.
     * @param limit O número máximo de resultados a retornar.
     * @return Uma lista de documentos do MongoDB representando as meditações recomendadas.
     * @throws RuntimeException se a busca no MongoDB falhar.
     */
    public List<Document> searchMeditationsByVector(List<Double> embedding, int limit) {
        logger.info("Realizando busca vetorial no MongoDB com embedding.");
        try {
            // Acessa o banco de dados e a coleção.
            // O nome do banco de dados agora é pego automaticamente das configurações
            MongoDatabase database = mongoClient.getDatabase("Atma-Contents"); // Usar o nome do BD do seu properties
            MongoCollection<Document> collection = database.getCollection("meditations"); // Mantenha o nome da sua coleção

            // Exemplo de consulta de busca vetorial para MongoDB Atlas Vector Search
            // Esta é uma representação simplificada. A consulta real pode ser mais complexa
            // dependendo de como você indexou seus dados no MongoDB Atlas.
            // O campo 'embedding_field' deve corresponder ao nome do campo em seus documentos
            // onde o vetor de embedding da meditação é armazenado.
            Document query = new Document("vectorSearch", embedding)
                    .append("path", "embedding_field") // O nome do campo que contém o vetor
                    .append("numCandidates", 100) // Número de candidatos a considerar (ajuste conforme necessário)
                    .append("limit", limit); // O número de resultados a retornar

            // Executa a agregação (pipeline) para a busca vetorial
            List<Document> results = collection.aggregate(
                    Collections.singletonList(
                            new Document("$vectorSearch", query)
                    )
            ).into(new java.util.ArrayList<>()); // Coleta os resultados em uma lista

            logger.info("Busca vetorial no MongoDB concluída. {} resultados encontrados.", results.size());
            return results;

        } catch (Exception e) {
            logger.error("Erro inesperado ao buscar meditações no MongoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar meditações por vetor: " + e.getMessage(), e);
        }
    }

    /**
     * Método principal de recomendação.
     * @param keywords Palavras-chave fornecidas pelo usuário para recomendação.
     * @param limit Limite de recomendações.
     * @return Uma lista de meditações recomendadas.
     */
    public List<Document> recommendMeditations(String keywords, int limit) {
        // 1. Gera o embedding das palavras-chave usando a API Python
        List<Double> embedding = generateEmbedding(keywords);

        // 2. Usa o embedding para buscar meditações similares no MongoDB
        List<Document> recommendedMeditations = searchMeditationsByVector(embedding, limit);

        return recommendedMeditations;
    }
}
