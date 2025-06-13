package compass.javaapirecommendation.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "meditations")
public class Meditation {

    @Id
    private String id;
    private String name;
    private String type;
    private List<String> keywords;
    private List<Double> embedding;


}
