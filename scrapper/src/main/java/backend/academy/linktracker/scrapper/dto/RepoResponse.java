package backend.academy.linktracker.scrapper.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RepoResponse {

    @JsonProperty("pushed_at")
    private Instant pushedAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("full_name")
    private String fullName;
}
