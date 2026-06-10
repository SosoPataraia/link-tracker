package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.scheduler")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SchedulerProperties {

    @Positive
    private long interval = 60000;

    /**
     * Number of links to load and process per scheduler tick. Must be between 50 and 500 per NFR.
     */
    @Min(50)
    @Max(500)
    private int batchSize = 100;

    /**
     * Number of parallel threads for processing one batch.
     */
    @Min(1)
    @Max(32)
    private int threadCount = 4;

    /**
     * When a link has never been checked, fetch updates from {@code now - fallbackWindow} instead of
     * the beginning of time, so the first check does not replay the entire history.
     */
    @NotNull
    private Duration fallbackWindow = Duration.ofDays(1);
}
