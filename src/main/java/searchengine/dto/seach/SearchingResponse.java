package searchengine.dto.seach;

import lombok.Data;

import java.util.List;

@Data
public class SearchingResponse {
    private boolean result;
    private int count;
    private List <SearchingData> data;
    private String error;

}
