package backend.academy.linktracker.scrapper.client;

import java.time.Instant;

public interface StackOverflowClient {
    Instant getLastActivity(long questionId);
}
