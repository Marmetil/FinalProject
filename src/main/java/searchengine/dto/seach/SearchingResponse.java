package searchengine.dto.seach;

import lombok.Data;

import java.util.List;

@Data
public class SearchingResponse {
    private boolean result;
    private List <SearchingData> searchingDataList;
    private String error;

}
