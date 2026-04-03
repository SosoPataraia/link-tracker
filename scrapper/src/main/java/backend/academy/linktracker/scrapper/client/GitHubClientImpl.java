package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.github.RepoResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClientImpl implements GitHubClient {

    private final RestClient gitHubRestClient;

    @Override
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
}
