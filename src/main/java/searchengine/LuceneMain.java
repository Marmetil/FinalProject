package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.services.LemmaCounter;
import searchengine.services.SearchingService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LuceneMain {


    public static void main(String[] args) throws IOException {
        System.out.println("");
//        LemmaCounter lemmaCounter = new LemmaCounter();
//        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
//        List<String> wordBaseForms = luceneMorph.getNormalForms("золотая");
//        wordBaseForms.forEach(System.out::println);
//        List<String> partOfSpeech = luceneMorph.getMorphInfo("по");
//        partOfSpeech.forEach(System.out::println);
//        System.out.println(lemmaCounter.splitTextIntoWords("шла саша по шоссе в магазин за красивыми игрушками"));
//        HashMap<String, Integer> lemmaCount = lemmaCounter.splitTextIntoWords("Шла Саша по шоссе, В красивый магазин");
//        lemmaCount.forEach((key, value) -> System.out.println(key + " - " + value));


    }

}
