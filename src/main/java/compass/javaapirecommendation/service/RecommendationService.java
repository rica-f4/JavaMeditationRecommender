package compass.javaapirecommendation.service;

import compass.javaapirecommendation.client.PythonEmbeddingsClient;
import compass.javaapirecommendation.entity.Meditation;
import compass.javaapirecommendation.entity.dto.MeditationDTO;
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
import java.util.stream.Collectors;


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
    private static final String MONGO_DATABASE_NAME = "Atma-Contents"; // Seu nome de banco de dados
    private static final String MONGO_COLLECTION_NAME = "meditations"; // Sua coleção real de meditações

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
    public List<Meditation> searchMeditationsByVector(List<Double> embedding, int limit) {
        logger.info("Realizando busca vetorial no MongoDB Atlas com embedding.");
        try {
            MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(MONGO_COLLECTION_NAME);

            // CORREÇÃO: O operador $vectorSearch espera o NOME DO ÍNDICE como primeiro argumento.
            // O vetor de busca, o campo e o limite são definidos DENTRO do objeto $vectorSearch.
            Document vectorSearchStage = new Document("$vectorSearch",
                    new Document("index", "meditations_vector_search") // O NOME do seu índice
                            .append("queryVector", embedding) // O vetor de busca
                            .append("path", "embedding") // O campo que contém o vetor no seu documento (corresponde ao seu modelo Meditation)
                            .append("numCandidates", 100) // Número de candidatos a considerar (ajuste para desempenho vs. precisão)
                            .append("limit", limit) // Limite de resultados a retornar
            );

            List<Document> results = collection.aggregate(
                    Collections.singletonList(vectorSearchStage)
            ).into(new java.util.ArrayList<>());

            logger.info("Busca vetorial no MongoDB concluída. {} resultados brutos encontrados.", results.size());

            // Mapeia os documentos resultantes para objetos Meditation
            return results.stream()
                    .map(doc -> {
                        Meditation meditation = new Meditation();
                        meditation.setId(doc.getString("_id"));
                        meditation.setName(doc.getString("name"));
                        meditation.setType(doc.getString("type"));
                        // A lista de keywords do MongoDB virá como List<String>
                        meditation.setKeywords(doc.get("keywords", List.class));
                        // A lista de embedding do MongoDB virá como List<Double> (se o driver mapear corretamente)
                        // ou List<Number>, então convertemos para List<Double>
                        List<?> rawEmbedding = doc.get("embedding", List.class);
                        if (rawEmbedding != null) {
                            meditation.setEmbedding(rawEmbedding.stream()
                                    .map(item -> ((Number) item).doubleValue())
                                    .collect(Collectors.toList()));
                        }
                        return meditation;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Erro inesperado ao buscar meditações no MongoDB: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar meditações por vetor: " + e.getMessage(), e);
        }
    }

    /**
     * Método principal de recomendação.
     * @param keywords Palavras-chave fornecidas pelo usuário para recomendação.
     * @param limit Limite de recomendações.
     * @return Uma lista de DTOs de meditações recomendadas.
     */
    public List<MeditationDTO> recommendMeditations(String keywords, int limit) {
        // 1. Gera o embedding das palavras-chave usando a API Python
        List<Double> embedding = generateEmbedding(keywords);

        // 2. Usa o embedding para buscar meditações similares no MongoDB
        List<Meditation> recommendedMeditations = searchMeditationsByVector(embedding, limit);

        // 3. Mapeia os objetos Meditation para DTOs antes de retornar
        return recommendedMeditations.stream()
                .map(med -> new MeditationDTO(med.getId(), med.getName(), med.getType())) // Uso do record DTO
                .collect(Collectors.toList());
    }
}
