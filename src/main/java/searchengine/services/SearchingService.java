package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;
import searchengine.dto.seach.AbsPageRelevance;
import searchengine.dto.seach.RelativePageRelevance;
import searchengine.config.Site;
import searchengine.dto.seach.SearchingData;
import searchengine.dto.seach.SearchingResponse;
import searchengine.model.*;
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
        List<String> requestedLemmas = splitTextIntoWords(text);
        List<String> uniqueLemmas = new ArrayList<>();
        int pageCount = (int) pageRepository.count();
        for (String lemma : requestedLemmas){
            int frequency = lemmaRepository.findByLemma(lemma).getFrequency();
            float quotient = (float) frequency /pageCount;
            if (quotient < 0.8  ){
                uniqueLemmas.add(lemma);
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

    private List findPages(String text, List<Site> siteList) throws IOException {
        try {
            List<Lemma> sortedLemmas = sortLemmas(text);
            Lemma firstLemma = sortedLemmas.get(0);
            Integer firstLemmaId = lemmaRepository.findByLemma(firstLemma.getLemma()).getId();
            List<Page> pageIdList = new ArrayList<>();
            for (Site site : siteList) {
                SiteEntity siteEntity = siteRepository.findIdByName(site.getName());
                List<Index> indexList = indexRepository.findIndexByLemmaAndSiteId(siteEntity.getId(), firstLemmaId);
                for (Index index : indexList) {
                    pageIdList.add(index.getPageId());
                }
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

        } catch (NullPointerException | ConcurrentModificationException | IndexOutOfBoundsException e){
            return null;
        }

    }

    public SearchingResponse getSearchingResponse(String query, List<Site> siteList, int offset, int limit) throws IOException {

            SearchingResponse searchingResponse = new SearchingResponse();
            List<RelativePageRelevance> pageListToResponse = findPages(query, siteList);
            List<SearchingData> searchingDataList = new ArrayList<>();
        try {

            if (pageListToResponse.isEmpty()) {
                searchingResponse.setResult(false);
                searchingResponse.setError("Задан пустой поисковый запрос");
                searchingResponse.setData(null);
            } else {
                searchingDataList = searchingDataListMaker(pageListToResponse, query);
                List<SearchingData> displaySearchingDataList = new ArrayList<>();
                if(offset + limit <= searchingDataList.size()) {
                    for (int i = offset; i < offset + limit; i++) {
                        displaySearchingDataList.add(searchingDataList.get(i));
                    }
                } else {
                    for (int i = offset; i < searchingDataList.size(); i++) {
                        displaySearchingDataList.add(searchingDataList.get(i));
                    }
                }

                searchingResponse.setResult(true);
                searchingResponse.setCount(searchingDataList.size());
                searchingResponse.setData(displaySearchingDataList);
            }
        }catch (NullPointerException e){
            searchingResponse.setResult(true);
            searchingResponse.setData(Collections.EMPTY_LIST);
        } return searchingResponse;
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
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        Map<WordPosition, List<String>> wordLemmaMap = new HashMap<>();
        String cleanText = text.replaceAll("[^\s^а-яА-Я]", "");
        String [] words = cleanText.toLowerCase().split("\s+");
        for (String word : words){
            int indexOfWord = text.toLowerCase().indexOf(word);
            List<String> lemmas = luceneMorph.getNormalForms(word);
            wordLemmaMap.put(new WordPosition(indexOfWord, word), lemmas);
        } return wordLemmaMap;
    }

    private Map<String, List<String>> requestedLemmasGetter(String query) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
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
                stringBuilder.append("...").append(partialSnippet).append(finish);
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
        } return searchingDataList;
    }

    private String wordsHighLighter(String snippet, Map<WordPosition, List<String>> requestedWordsLemmaMap) throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
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
        } return snippet;
    }
}


