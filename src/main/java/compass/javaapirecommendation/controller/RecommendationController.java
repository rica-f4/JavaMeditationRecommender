package compass.javaapirecommendation.controller;

// Exemplo de Controller em compass.meditationrecommender.controller


import compass.javaapirecommendation.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.Document;
import java.util.List;

@RestController
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;

    @Autowired
    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<Document>> getRecommendations(@RequestParam String keywords,
                                                             @RequestParam(defaultValue = "5") int limit) {
        logger.info("Requisição de recomendação recebida para keywords: '{}' com limite: {}", keywords, limit);
        try {
            List<Document> recommendations = recommendationService.recommendMeditations(keywords, limit);
            if (recommendations.isEmpty()) {
                logger.warn("Nenhuma recomendação encontrada para keywords: {}", keywords);
                return ResponseEntity.noContent().build(); // Retorna 204 No Content
            }
            logger.info("Retornando {} recomendações para keywords: {}", recommendations.size(), keywords);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            logger.error("Erro ao processar requisição de recomendação: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null); // Retorna 500 Internal Server Error
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("Health check endpoint called");
        return ResponseEntity.ok("Service is running");
    }
}
