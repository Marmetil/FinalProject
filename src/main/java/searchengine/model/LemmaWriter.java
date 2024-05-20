package searchengine.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.LemmaCounter;

import java.io.IOException;


@RequiredArgsConstructor
public class LemmaWriter extends Thread{
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final String link;

    @Override
    public void run(){
        LemmaCounter lemmaCounter = new LemmaCounter(siteRepository, pageRepository, sitesList, lemmaRepository, indexRepository);
        try {
            lemmaCounter.IndexAllPage(link);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
