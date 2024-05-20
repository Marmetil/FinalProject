package searchengine.dto.seach;

import lombok.Data;

@Data
public class SearchingData {
    private String url;
    private String title;
    private String snippet;
    private  float relevance;
}
