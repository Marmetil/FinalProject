package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.seach.AbsPageRelevance;
import searchengine.dto.seach.RelativePageRelevance;
import searchengine.config.Site;
import searchengine.dto.seach.SearchingData;
import searchengine.dto.seach.SearchingResponse;
import searchengine.model.*;
import searchengine.repositories.*;
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
    private LuceneMorphology luceneMorph;
    {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> splitTextIntoWords(String query) throws IOException {
        query = query.replaceAll("[^\s^а-яА-Я]", "");
        String [] words = query.toLowerCase().split("\s+");
        ArrayList<String> allWords = new ArrayList<>(Arrays.asList(words));
        ArrayList<String> allLemmas = new ArrayList<>();
        HashMap<String, Integer> lemmaCount = new HashMap<>();
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

    private List<String> excludeFrequentLemma(String query, SiteEntity siteEntity) throws IOException {
        List<String> requestedLemmas = splitTextIntoWords(query);
        List<String> uniqueLemmas = new ArrayList<>();
        int pageCount = pageRepository.countBySiteId(siteEntity);
        for (String lemma : requestedLemmas){
            int frequency = lemmaRepository.findByLemmaAndSiteId(lemma, siteEntity).getFrequency();
            float quotient = (float) frequency /pageCount;
            if (quotient < 0.8  ){
                uniqueLemmas.add(lemma);
            }
        } return uniqueLemmas;
    }

    private List<Lemma> sortLemmas(String query, SiteEntity siteEntity) throws IOException {
        List<String> uniqueLemmas = excludeFrequentLemma(query, siteEntity);
        List<Lemma> sortedLemmas = new ArrayList<>();
        for (String lemma : uniqueLemmas){
            Lemma lemma1 = lemmaRepository.findByLemmaAndSiteId(lemma, siteEntity);
            sortedLemmas.add(lemma1);
        }
        sortedLemmas.sort(Comparator.comparing(Lemma::getFrequency));
       return sortedLemmas;
    }

    private List findPages(String text, SiteEntity siteEntity) throws IOException {
        try {
            List<Page> pageIdList = new ArrayList<>();
            List<Lemma> sortedLemmas = sortLemmas(text, siteEntity);
            Lemma firstLemma = sortedLemmas.get(0);
            Integer firstLemmaId = lemmaRepository.findByLemmaAndSiteId(firstLemma.getLemma(), siteEntity).getId();
            List<Index> indexList = indexRepository.findIndexByLemmaAndSiteId(siteEntity.getId(), firstLemmaId);
            for (Index index : indexList) {
                pageIdList.add(index.getPageId());
            }
            for (int i = 1; i < sortedLemmas.size(); i++) {
                List<Page> newPageIdList = new ArrayList<>();
                for (Page page : pageIdList) {
                    Index index = indexRepository.findByLemmaIdAndPageId(sortedLemmas.get(i), page);
                    if (!(index == null)) {
                        newPageIdList.add(index.getPageId());
                    }
                    pageIdList = newPageIdList;
                }
            }
            if (pageIdList.isEmpty()) {
                return Collections.EMPTY_LIST;
            } else {
                return relativePageGetter(pageIdList, sortedLemmas);
            }
        } catch (NullPointerException | ConcurrentModificationException | IndexOutOfBoundsException e){
            return null;
        }
    }

        public SearchingResponse getSearchingResponse(String query, List<Site> siteList, int offset, int limit) throws IOException {
            SearchingResponse searchingResponse = new SearchingResponse();
            List<RelativePageRelevance> pageListToResponse = new ArrayList<>();
            for(Site site : siteList){
                List<RelativePageRelevance> sitePageListToResponse = findPages(query, siteRepository.findIdByName(site.getName()));
                if(sitePageListToResponse != null){
                    pageListToResponse.addAll(sitePageListToResponse);
                }
            }
            try {
                if (pageListToResponse.isEmpty()) {
                searchingResponse.setResult(false);
                searchingResponse.setError("Задан пустой поисковый запрос");
                searchingResponse.setData(null);
                } else {
                    List<SearchingData> displaySearchingDataList = new ArrayList<>();
                    if(offset + limit <= pageListToResponse.size()){
                        List<RelativePageRelevance> newPageListToResponse = new ArrayList<>();
                        for(int i = offset; i < offset + limit; i++){
                            newPageListToResponse.add(pageListToResponse.get(i));
                        }
                        displaySearchingDataList = searchingDataListMaker(newPageListToResponse,query);
                    }  else {
                        displaySearchingDataList = searchingDataListMaker(pageListToResponse, query);
                    }
                searchingResponse.setResult(true);
                searchingResponse.setCount(pageListToResponse.size());
                searchingResponse.setData(displaySearchingDataList);
            }
        }catch (NullPointerException e){
            searchingResponse.setResult(true);
            searchingResponse.setData(Collections.EMPTY_LIST);
        }
        return searchingResponse;
    }

    private String pageTitleGetter(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        return document.title();
    }

 private String snippetGetter(String query, String pageUrl) throws IOException {
        String text = pageTextGetter(pageUrl);
        Map<WordPosition, List<String>> wordLemmaMap = wordLemmaMapMaker(text);
        Map<WordPosition, List<String>> requestedWordsLemmaMap = new HashMap<>();
        Map<String, List<String>> requestedLemmas = requestedLemmasGetter(query);
        int snippetSize = 300;
        int partialSnippetSize = snippetSize/ requestedLemmas.size();

       List<WordPosition> sortedRequestedWords = new ArrayList<>();
          for (Map.Entry<String, List<String>> entry : requestedLemmas.entrySet()){
            for (Map.Entry<WordPosition, List<String>> entry1 : wordLemmaMap.entrySet()){
                if(entry.getValue().equals(entry1.getValue())){
                    sortedRequestedWords.add(entry1.getKey());
                    requestedWordsLemmaMap.put(entry1.getKey(), entry.getValue());
                    break;
                }
            }
        }
        sortedRequestedWords.sort(Comparator.comparing(WordPosition::getIndexOfWord));
        String snippet = snippetMaker(sortedRequestedWords, text, snippetSize, partialSnippetSize);
        return wordsHighLighter(snippet, requestedWordsLemmaMap);
    }
    private String pageTextGetter(String url) throws IOException {
        Document document = Jsoup.connect(url).get();
        String htmlText = document.html();
        return Jsoup.parse(htmlText).text();
    }

    private Map<WordPosition, List<String>> wordLemmaMapMaker(String text) throws IOException {
        Map<WordPosition, List<String>> wordLemmaMap = new HashMap<>();
        String cleanText = text.replaceAll("[^\s^а-яА-Я]", "");
        String [] words = cleanText.toLowerCase().split("\s+");
        for (String word : words){
            try {
                int indexOfWord = text.toLowerCase().indexOf(word);
                List<String> lemmas = luceneMorph.getNormalForms(word);
                wordLemmaMap.put(new WordPosition(indexOfWord, word), lemmas);
            }catch (ArrayIndexOutOfBoundsException e){
                continue;
            }
        }
        return wordLemmaMap;
    }
    private Map<String, List<String>> requestedLemmasGetter(String query) throws IOException {
        Map<String, List<String>> requestedLemmas = new HashMap<>();
        String[] requestedWords = query.split("\s+");
        for (String word : requestedWords){
            List<String> lemmas = luceneMorph.getNormalForms(word);
            requestedLemmas.put(word, lemmas);
        }
        return requestedLemmas;
    }
    private String snippetMaker(List<WordPosition> sortedRequestedWords, String text, int snippetSize, int partialSnippetSize){
        StringBuilder stringBuilder = new StringBuilder();
        String partialSnippet;
        int start;
        int end;
        if(sortedRequestedWords.size() == 1){
            int firstIndex = sortedRequestedWords.get(0).getIndexOfWord() - (snippetSize / 2);
            start = firstIndex < 0 ? 0 : text.indexOf(" ", firstIndex) + 1;
            end = text.indexOf(" ", start + snippetSize);
            partialSnippet = text.substring(start, end);
            stringBuilder.append("...").append(partialSnippet).append("...");
        } else {
            for (int i = 0; i < sortedRequestedWords.size(); i++) {
                start = sortedRequestedWords.get(i).getIndexOfWord();
                end = text.indexOf(" ", start + partialSnippetSize);
                partialSnippet = text.substring(start, end);
                String finish = (i == sortedRequestedWords.size() - 1 ? "..." : "");
                stringBuilder.append("... ").append(partialSnippet).append(finish);
            }
        }
        return stringBuilder.toString();
    }
    private List<SearchingData> searchingDataListMaker(List<RelativePageRelevance> pageListToResponse, String query) throws IOException {
        List<SearchingData> searchingDataList = new ArrayList<>();
        for (RelativePageRelevance relativePageRelevance : pageListToResponse) {
            String siteUrl = relativePageRelevance.getPage().getSiteId().getUrl();
            String pagePath = relativePageRelevance.getPage().getPath();
            String pageUrl = siteUrl + pagePath;
            SearchingData searchingData = new SearchingData();
            searchingData.setSite(siteUrl);
            searchingData.setSiteName(relativePageRelevance.getPage().getSiteId().getName());
            searchingData.setRelevance(relativePageRelevance.getRelativeRelevance());
            searchingData.setTitle(pageTitleGetter(pageUrl));
            searchingData.setSnippet(snippetGetter(query, pageUrl));
            searchingData.setUrl(relativePageRelevance.getPage().getPath());
            searchingDataList.add(searchingData);
        }
        return searchingDataList;
    }

    private String wordsHighLighter(String snippet, Map<WordPosition, List<String>> requestedWordsLemmaMap) throws IOException {
        String snippet1 = snippet.replaceAll("[^\s^а-яА-Я]", "");
        String[] snippetWords = snippet1.split("\s+");
        Map<String, List<String>> sippetLemmasMap = new HashMap<>();
        for (String snippetWord : snippetWords){
            try {
                List<String> partOfSpeech = luceneMorph.getMorphInfo(snippetWord.toLowerCase());
                partOfSpeech.stream().filter(p -> !p.toUpperCase().contains("СОЮЗ") && !p.toUpperCase().contains("МЕЖД") && !p.toUpperCase().contains("ПРЕДЛ"))
                        .map(p -> luceneMorph.getNormalForms(snippetWord.toLowerCase()))
                        .forEach(lemmas -> sippetLemmasMap.put(snippetWord, lemmas));
            }  catch (ArrayIndexOutOfBoundsException | WrongCharaterException exception){
                continue;
            }
        }
        for (Map.Entry<String, List<String>> entry : sippetLemmasMap.entrySet()){
            for (Map.Entry<WordPosition, List<String>> entry1 : requestedWordsLemmaMap.entrySet()){
                if(entry.getValue().equals(entry1.getValue())){
                    snippet = snippet.replaceAll(entry.getKey(), "<b>" + entry.getKey() + "</b>");
                }
            }
        }
        return snippet;
    }
    private List<RelativePageRelevance> relativePageGetter(List<Page> pageIdList, List<Lemma> sortedLemmas) {
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
        for (AbsPageRelevance absPageRelevance : absPageRelevanceList) {
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


