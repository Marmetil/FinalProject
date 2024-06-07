package searchengine.services;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import searchengine.config.Referrer;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.model.*;
import searchengine.repositories.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LemmaCounter {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList siteList;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public HashMap<String, Integer> splitTextIntoWords(String text) throws IOException {
        text = text.replaceAll("[^\s^а-яА-Я]", "");
        String [] words = text.toLowerCase().split("\s+");
        ArrayList<String> allWords = new ArrayList<>(Arrays.asList(words));
        ArrayList<String> allLemmas = new ArrayList<>();

        HashMap<String, Integer> lemmaCount = new HashMap<>();
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();

        for (String word : allWords){
            List<String> partOfSpeech = luceneMorph.getMorphInfo(word);
            partOfSpeech.forEach(p-> {
            if (!p.toUpperCase().contains("СОЮЗ") && !p.toUpperCase().contains("МЕЖД") && !p.toUpperCase().contains("ПРЕДЛ")) {
                    List<String> lemmas = luceneMorph.getNormalForms(word);
                    allLemmas.addAll(lemmas);
                }
            });
            }
        for (String lemma: allLemmas){
            if(!lemmaCount.containsKey(lemma)){
                lemmaCount.put(lemma, 0);
            }
            lemmaCount.put(lemma, lemmaCount.get(lemma) + 1);
        }

        return lemmaCount;
    }
    public void IndexAllPage(String url) throws IOException {
        String htmlText = htmlGetter(url);
        String text = htmlToText(htmlText);
        HashMap<String, Integer> lemmaCount = splitTextIntoWords(text);
        fillInLemma(lemmaCount, siteRepository.findIdByUrl(urlRootFinder(url)));
        fillInIndex(lemmaCount, pageRepository.findIdByPath(pathFinder(url)));
        log.info("Леммы записаны");
    }
    public String urlRootFinder(String url){
        String[] partsUrl = url.split("/");
        return partsUrl[0] + "//" + partsUrl[2];
    }
    private static String pathFinder(String url){
        String[] partsUrl = url.split("/");

        int start = partsUrl[0].length() + partsUrl[2].length() + 3;
        return "/" + url.substring(start);
    }
    private int responseCodeGetter(String url) throws IOException {
        URL url1 = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)url1.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }

    private String htmlGetter(String url) throws IOException{
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();
        return document.html();
    }
        private Site makeSite(SiteEntity siteEntity){
            Site site = new Site();
            site.setName(siteEntity.getName());
            site.setUrl(siteEntity.getUrl());
            return site;
        }
    private synchronized void fillInLemma(HashMap<String, Integer> lemmaCount, SiteEntity siteId){
        lemmaCount.forEach((key, value) -> {
            Lemma lemma = lemmaRepository.findByLemma(key);
            if (lemma == null){
                Lemma newLemma = new Lemma();
                newLemma.setLemma(key);
                newLemma.setSiteId(siteId);
                newLemma.setFrequency(1);
                lemmaRepository.save(newLemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);
                log.info("Записана лемма " + lemma.getLemma() + " " + lemma.getId() + " частота " + lemma.getFrequency());
            }
        });
    }

    private void fillInIndex(HashMap<String, Integer> lemmaCount, Page pageId){
        lemmaCount.forEach((key, value) -> {
            Index index = new Index();
            index.setPageId(pageId);
            index.setLemmaId(lemmaRepository.findByLemma(key));
            index.setRank(value);
            indexRepository.save(index);
            log.info("Записан индекс по странице " + pageId.getId() + " лемма " + lemmaRepository.findByLemma(key).getId() );
        });
    }

    public String htmlToText(String html) {
        return Jsoup.parse(html).text();
    }
    public void IndexPage(String url) throws IOException {
        SiteEntity siteEntity = siteRepository.findIdByUrl(urlRootFinder(url));
        if(siteEntity == null){
            for (Site site : siteList.getSites()){
                if(site.getUrl().equals(urlRootFinder(url))){
                    SiteEntity newSiteEntity = new SiteEntity();
                    newSiteEntity.setStatus(Status.INDEXING);
                    newSiteEntity.setUrl(urlRootFinder(url));
                    newSiteEntity.setName(site.getName());
                    newSiteEntity.setLastError(null);
                    newSiteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(newSiteEntity);
                }
            }
        }

        Page page = pageRepository.findIdByPath(pathFinder(url));
        String htmlText = htmlGetter(url);
        if (!(page == null)){

            List<Lemma> lemmasToUpdate = lemmaRepository.lemmaToUpdate(page.getId());
            for (Lemma lemma : lemmasToUpdate){
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaRepository.save(lemma);
            }
            indexRepository.deleteIndex(page);
            lemmaRepository.deleteLemma();
            pageRepository.delete(page);
        }
        Page newPage = new Page();
        newPage.setCode(responseCodeGetter(url));
        newPage.setContent(htmlText);
        newPage.setPath(pathFinder(url));
        newPage.setSiteId(siteRepository.findIdByUrl(urlRootFinder(url)));
        pageRepository.save(newPage);

        String text = htmlToText(htmlText);
        HashMap<String, Integer> lemmaCount = splitTextIntoWords(text);
        fillInLemma(lemmaCount, siteRepository.findIdByUrl(urlRootFinder(url)));
        fillInIndex(lemmaCount, pageRepository.findIdByPath(pathFinder(url)));
        log.info("Запись лемм завершена");

    }

}



