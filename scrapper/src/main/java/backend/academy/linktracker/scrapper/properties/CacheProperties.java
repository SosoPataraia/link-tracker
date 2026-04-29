package backend.academy.linktracker.scrapper.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
@NoArgsConstructor
public class CacheProperties {
    private Duration ttl = Duration.ofSeconds(60);
}
