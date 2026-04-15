package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.dto.github.RepoResponse;
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

    @Override
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
            log.error("GitHub issues API error for {}/{}: {}", owner, repo, e.getMessage());
            return List.of();
        }
    }

    @Override
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
            log.error("GitHub pulls API error for {}/{}: {}", owner, repo, e.getMessage());
            return List.of();
        }
    }
}
