package searchengine.model;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.CopyOnWriteArrayList;
@Component
@NoArgsConstructor
public class SiteMap {
    private String url;
    private CopyOnWriteArrayList<SiteMap> childLinks;

    public SiteMap (String url) {
        childLinks = new CopyOnWriteArrayList<>();
        this.url = url;
    }

    public void addChild (SiteMap child) {

        childLinks.add(child);
    }
    public CopyOnWriteArrayList<SiteMap> getChildLinks(){

        return childLinks;
    }
    public String getUrl(){
        return url;
    }
}
