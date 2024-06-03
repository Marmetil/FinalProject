package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Getter
@Setter
public class WordPosition {
    private int indexOfWord;
    private String word;
    public WordPosition(int indexOfWord, String word){
        this.indexOfWord = indexOfWord;
        this.word = word;
    }
}
