package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.database")
@Getter
@Setter
@NoArgsConstructor
public class DatabaseProperties {

    private AccessType accessType = AccessType.SQL;

    public enum AccessType {
        SQL, ORM
    }
}
