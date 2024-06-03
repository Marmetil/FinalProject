package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "referrer")
public class Referrer {
    private String referrer;
}
