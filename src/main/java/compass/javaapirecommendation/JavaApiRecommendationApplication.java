package compass.javaapirecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class JavaApiRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaApiRecommendationApplication.class, args);
    }

}
