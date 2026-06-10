package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import java.time.Instant;
import java.util.List;

public interface GitHubClient {

    /**
     * Returns the latest push timestamp for the repo (staleness fallback). Returns {@code null} on error.
     */
    Instant getLastUpdated(String owner, String repo);

    /**
     * Single call to the GitHub issues endpoint, which also returns pull requests
     * ({@link IssueItem#isPullRequest()} tells them apart). Returns items updated since {@code since}.
     *
     * @throws org.springframework.web.client.RestClientException if the call fails, so the caller can
     *     distinguish "API unavailable" from "nothing new" and avoid advancing last-checked.
     */
    List<IssueItem> getIssuesAndPullRequests(String owner, String repo, Instant since);
}
