package compass.javaapirecommendation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "python-embedding-service", url = "${python.api.url}")
public interface PythonEmbeddingsClient {


        @PostMapping(value = "/embed", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        List<Double> getEmbedding(@RequestBody Map<String, String> requestBody);
    }

