package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import java.time.Instant;
import java.util.List;

public interface GitHubClient {

    /**
     * Returns the latest push timestamp for the repo (used as a fallback / staleness check).
     */
    Instant getLastUpdated(String owner, String repo);

    /**
     * Returns issues (not PRs) created after {@code since}, newest first.
     */
    List<IssueItem> getNewIssues(String owner, String repo, Instant since);

    /**
     * Returns pull requests created after {@code since}, newest first.
     */
    List<IssueItem> getNewPullRequests(String owner, String repo, Instant since);
}
