package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.resilience")
@Validated
@Getter
@Setter
public class ResilienceProperties {

    private boolean retryExponentialBackoffEnabled = false;

    private double retryExponentialBackoffMultiplier = 2.0;

    @NotEmpty
    private List<Integer> retryableStatusCodes = List.of(500, 502, 503, 504);

    @NotNull
    private Duration retryWaitDuration = Duration.ofMillis(500);

    @NotNull
    private Integer retryMaxAttempts = 3;
}
