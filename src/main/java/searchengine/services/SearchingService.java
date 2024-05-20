package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.dto.seach.AbsPageRelevance;
import searchengine.dto.seach.RelativePageRelevance;
import searchengine.config.Site;
import searchengine.dto.seach.SearchingData;
import searchengine.dto.seach.SearchingResponse;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchingService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private List<String> splitTextIntoWords(String text) throws IOException {
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
        List<String> requestedLemmas = new ArrayList<>();
        lemmaCount.forEach((key, value) -> requestedLemmas.add(key));

        return requestedLemmas;
    }

    private List<String> excludeFrequentLemma(String text) throws IOException {
       List<String> uniqueLemmas = splitTextIntoWords(text);
        int pageCount = (int) pageRepository.count();
        for (String lemma : uniqueLemmas){
            int frequency = lemmaRepository.findByLemma(lemma).getFrequency();
            float quotient = (float) frequency /pageCount;
            if (quotient >= 0.7){
                uniqueLemmas.remove(lemma);
            }
        } return uniqueLemmas;
    }

    private List<Lemma> sortLemmas(String text) throws IOException {
        List<String> uniqueLemmas = excludeFrequentLemma(text);
        List<Lemma> sortedLemmas = new ArrayList<>();
        for (String lemma : uniqueLemmas){
            Lemma lemma1 = lemmaRepository.findByLemma(lemma);
            sortedLemmas.add(lemma1);
        }
        sortedLemmas.sort(Comparator.comparing(Lemma::getFrequency));
       return sortedLemmas;
    }

    private List<RelativePageRelevance> findPages(String text, List<Site> siteList) throws IOException {
        List<Lemma> sortedLemmas = sortLemmas(text);
        Lemma firstLemma = sortedLemmas.get(0);
        Integer firstLemmaId = lemmaRepository.findByLemma(firstLemma.getLemma()).getId();
        List<Page> pageIdList = new ArrayList<>();
        for(Site site : siteList) {
            SiteEntity siteEntity = siteRepository.findIdByName(site.getName());
            List<Index> indexList = indexRepository.findIndexByLemmaAndSiteId(siteEntity.getId(), firstLemmaId);
            for (Index index : indexList) {
                pageIdList.add(index.getPageId());
            }
        }
        for (int i = 1; i < sortedLemmas.size(); i++) {
            for (Page page : pageIdList) {
                Index index = indexRepository.findByLemmaIdAndPageId(sortedLemmas.get(i), page);
                if (index == null) {
                    pageIdList.remove(page);
                }
            }
        }
        if (pageIdList.isEmpty()) {
            return null;
        } else {
            List<RelativePageRelevance> relativePageRelevanceList = new ArrayList<>();
            float rank = 0;
            List<AbsPageRelevance> absPageRelevanceList = new ArrayList<>();
            for (Page page : pageIdList) {
                for (Lemma lemma : sortedLemmas) {
                    rank = rank + indexRepository.findByLemmaIdAndPageId(lemma, page).getRank();
                }
                float absRelevance = rank;
                AbsPageRelevance absPageRelevance = new AbsPageRelevance(page, absRelevance);
                  absPageRelevanceList.add(absPageRelevance);
            }
            absPageRelevanceList.sort(Comparator.comparing(AbsPageRelevance::getAbsRelevance));

            for (AbsPageRelevance absPageRelevance : absPageRelevanceList){
                AbsPageRelevance maxAbsPageRelevance = absPageRelevanceList.get(absPageRelevanceList.size() - 1);
                float maxAbsRelevance = maxAbsPageRelevance.getAbsRelevance();
                float relativeRelevance = absPageRelevance.getAbsRelevance() / maxAbsRelevance;
                RelativePageRelevance relativePageRelevance = new RelativePageRelevance(absPageRelevance.getPage(), relativeRelevance);
                relativePageRelevanceList.add(relativePageRelevance);
            }
            relativePageRelevanceList.sort(Comparator.comparing(RelativePageRelevance::getRelativeRelevance).reversed());
            return relativePageRelevanceList;
        }

    }

    public SearchingResponse getSearchingResponse(String query, List<Site> siteList) throws IOException {
        SearchingResponse searchingResponse = new SearchingResponse();
        List<RelativePageRelevance> pageListToResponse = findPages(query, siteList);
        List<SearchingData> searchingDataList = new ArrayList<>();
        if(pageListToResponse == null){
            searchingResponse.setResult(false);
            searchingResponse.setError("Задан пустой поисковый запрос");
            searchingResponse.setSearchingDataList(null);
        } else {
            for (RelativePageRelevance relativePageRelevance : pageListToResponse){
                SearchingData searchingData = new SearchingData();
                searchingData.setRelevance(relativePageRelevance.getRelativeRelevance());
                searchingData.setTitle(null);
                searchingData.setSnippet(null);
                searchingData.setUrl(relativePageRelevance.getPage().getPath());
                searchingDataList.add(searchingData);
            }
            searchingResponse.setResult(true);
            searchingResponse.setSearchingDataList(searchingDataList);
        } return searchingResponse;
    }
}
