package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LinkRepository {

    TrackedLink save(TrackedLink link);

    Optional<TrackedLink> findById(long id);

    Optional<TrackedLink> findByChatAndUrl(long chatId, String url);

    List<TrackedLink> findAllByChat(long chatId);

    Collection<TrackedLink> findAll();

    boolean remove(long chatId, String url);

    void removeAllByChat(long chatId);

    /** Advances last-checked for the given links in a single transaction. */
    void updateLastChecked(Collection<Long> linkIds, Instant lastChecked);

    /**
     * Keyset page of distinct links due for checking: last-checked before {@code checkedBefore}
     * (nulls treated as the epoch), ordered by (last_checked, id), starting strictly after the cursor.
     * Returns link rows only — no chats or tags — to avoid N+1. Stable under concurrent last-checked
     * updates because processed links jump to now (past checkedBefore) and leave the candidate set.
     */
    List<TrackedLink> findLinksToCheck(Instant checkedBefore, Instant cursorLastChecked, long cursorId, int limit);

    /** Subscriber chat ids for the given links, loaded in one query. */
    Map<Long, List<Long>> findChatIdsByLinkIds(Collection<Long> linkIds);
}
