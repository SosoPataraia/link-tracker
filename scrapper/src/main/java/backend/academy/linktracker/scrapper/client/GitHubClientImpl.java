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
<<<<<<< HEAD
                    .log("github.getLastUpdated.failed");
            throw e;
=======
                    .log("github.repo.fetch.failed");
            return null;
>>>>>>> 8298d9d (refactor: apply structured logging, @Data DTOs, TelegramBotAdapter, Dead code cleanup, unused config)
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
<<<<<<< HEAD
                    .log("github.getNewIssues.failed");
            throw e;
=======
                    .log("github.issues.fetch.failed");
            return List.of();
>>>>>>> 8298d9d (refactor: apply structured logging, @Data DTOs, TelegramBotAdapter, Dead code cleanup, unused config)
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
<<<<<<< HEAD
                    .log("github.getNewPullRequests.failed");
            throw e;
=======
                    .log("github.pulls.fetch.failed");
            return List.of();
>>>>>>> 8298d9d (refactor: apply structured logging, @Data DTOs, TelegramBotAdapter, Dead code cleanup, unused config)
        }
    }
}
