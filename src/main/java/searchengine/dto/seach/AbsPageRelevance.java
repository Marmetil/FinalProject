package searchengine.dto.seach;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import searchengine.model.Page;

@Setter
@Getter
@Data
public class AbsPageRelevance {
    private Page page;
    private float absRelevance;

    public AbsPageRelevance(Page page, float absRelevance){
        this.page = page;
        this.absRelevance = absRelevance;
    }
}
