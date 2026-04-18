package backend.academy.linktracker.bot.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
@Getter
@Setter
@NoArgsConstructor
public class KafkaProperties {

    private Topic topic = new Topic();
    private Consumer consumer = new Consumer();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Topic {
        private String linkUpdates = "link-updates";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Consumer {
        private int retryAttempts = 3;
    }
}
