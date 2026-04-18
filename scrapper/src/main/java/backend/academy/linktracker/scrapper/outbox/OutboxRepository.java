package backend.academy.linktracker.scrapper.outbox;

import java.util.List;

public interface OutboxRepository {
    void save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    void markProcessed(long id);

    void markFailed(long id);
}
