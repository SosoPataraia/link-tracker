package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {

    TrackedLink save(TrackedLink link);

    Optional<TrackedLink> findById(long id);

    Optional<TrackedLink> findByChatAndUrl(long chatId, String url);

    List<TrackedLink> findAllByChat(long chatId);

    Collection<TrackedLink> findAll();

    boolean remove(long chatId, String url);

    void removeAllByChat(long chatId);

    /**
     * Updates last_checked for all rows sharing this link id.
     * Called by the scheduler after each successful check.
     */
    void updateLastChecked(long linkId, Instant lastChecked);

    /**
     * Returns up to {@code limit} distinct links ordered by last_checked ASC (nulls first),
     * so the stalest links are always processed first.
     * {@code offset} is used for pagination across batches in a single scheduler tick.
     */
    List<TrackedLink> findBatch(int offset, int limit);
}
