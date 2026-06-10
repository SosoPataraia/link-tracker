package backend.academy.linktracker.bot.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.kafka")
@Validated
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
        private String linkUpdatesDlt = "link-updates.DLT";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Consumer {
        @Min(1)
        private int retryAttempts = 3;
    }
}
