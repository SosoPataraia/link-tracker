package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.dto.github.RepoResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubClientImpl implements GitHubClient {

    private static final String CIRCUIT_BREAKER_NAME = "githubClient";

    private final RestClient gitHubRestClient;

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public Instant getLastUpdated(String owner, String repo) {
        try {
            var response = gitHubRestClient
                    .get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .body(RepoResponse.class);
            return response != null ? response.getPushedAt() : null;
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("owner", owner)
                    .addKeyValue("repo", repo)
                    .addKeyValue("error", e.getMessage())
                    .log("github.repo.fetch.failed");
            throw e;
        }
    }

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public List<IssueItem> getNewIssues(String owner, String repo, Instant since) {
        try {
            List<IssueItem> items = gitHubRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", "open")
                            .queryParam("since", since.toString())
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", "50")
                            .build(owner, repo))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<IssueItem>>() {});
            if (items == null) return List.of();
            return items.stream().filter(i -> !i.isPullRequest()).toList();
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("owner", owner)
                    .addKeyValue("repo", repo)
                    .addKeyValue("error", e.getMessage())
                    .log("github.issues.fetch.failed");
            throw e;
        }
    }

    @Override
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
    public List<IssueItem> getNewPullRequests(String owner, String repo, Instant since) {
        try {
            List<IssueItem> items = gitHubRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls")
                            .queryParam("state", "open")
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", "50")
                            .build(owner, repo))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<IssueItem>>() {});
            if (items == null) return List.of();
            return items.stream()
                    .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(since))
                    .toList();
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("owner", owner)
                    .addKeyValue("repo", repo)
                    .addKeyValue("error", e.getMessage())
                    .log("github.pulls.fetch.failed");
            throw e;
        }
    }
}
