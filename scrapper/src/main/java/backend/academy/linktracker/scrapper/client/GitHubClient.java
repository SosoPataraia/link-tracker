package backend.academy.linktracker.scrapper.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final RestClient gitHubRestClient;

    /**
     * Returns last updated time for a GitHub repository.
     * URL format: https://github.com/{owner}/{repo}
     */
    public Instant getLastUpdated(String owner, String repo) {
        try {
            var response = gitHubRestClient
                    .get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .body(RepoResponse.class);
            return response != null ? response.getPushedAt() : null;
        } catch (RestClientException e) {
            log.error("GitHub API error for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RepoResponse {

        @JsonProperty("pushed_at")
        private Instant pushedAt;

        @JsonProperty("updated_at")
        private Instant updatedAt;

        @JsonProperty("full_name")
        private String fullName;
    }
}
