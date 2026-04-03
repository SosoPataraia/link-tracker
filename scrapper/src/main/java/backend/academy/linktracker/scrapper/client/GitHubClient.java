package backend.academy.linktracker.scrapper.client;

import java.time.Instant;

public interface GitHubClient {
    Instant getLastUpdated(String owner, String repo);
}
