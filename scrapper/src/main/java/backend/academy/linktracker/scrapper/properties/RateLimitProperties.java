package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
@Getter
@Setter
public class RateLimitProperties {

    @Positive
    private int capacity = 50;

    @Positive
    private int refillTokens = 50;

    @NotNull
    private Duration refillPeriod = Duration.ofMinutes(1);
}
