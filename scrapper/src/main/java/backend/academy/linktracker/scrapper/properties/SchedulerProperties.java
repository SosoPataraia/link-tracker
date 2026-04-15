package backend.academy.linktracker.scrapper.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SchedulerProperties {

    private long interval = 60000;

    /**
     * Number of links to load and process per scheduler tick.
     * Must be between 50 and 500 per NFR.
     */
    private int batchSize = 100;

    /**
     * Number of parallel threads for processing one batch.
     * Each thread processes batchSize / threadCount links.
     */
    private int threadCount = 4;
}
