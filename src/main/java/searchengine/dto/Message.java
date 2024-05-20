package searchengine.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;


@Setter
@Getter
public class Message {
        private boolean result;
        private String error;

    public Message(boolean result) {
        this.result = result;
    }

    public Message(boolean result, String error) {
        this.result = result;
        this.error = error;
    }
}
