package searchengine.dto.seach;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.model.Page;
@Setter
@Getter
@Data
public class RelativePageRelevance {
    private Page page;
    private float relativeRelevance;

    public RelativePageRelevance(Page page, float relativeRelevance){
        this.page = page;
        this.relativeRelevance = relativeRelevance;
    }
}
