package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.jpa.LinkEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LinkJpaRepository extends JpaRepository<LinkEntity, Long> {

    Optional<LinkEntity> findByUrl(String url);

    @Query("""
            SELECT l FROM LinkEntity l
            JOIN l.chats c
            WHERE c.id = :chatId
            """)
    List<LinkEntity> findAllByChatsId(long chatId);

    @Query("""
            SELECT l FROM LinkEntity l
            JOIN l.chats c
            WHERE c.id = :chatId AND l.url = :url
            """)
    Optional<LinkEntity> findByChatIdAndUrl(long chatId, String url);

    @Query("""
            SELECT l FROM LinkEntity l
            WHERE COALESCE(l.lastChecked, :epoch) < :checkedBefore
              AND ( COALESCE(l.lastChecked, :epoch) > :cursorLastChecked
                    OR (COALESCE(l.lastChecked, :epoch) = :cursorLastChecked AND l.id > :cursorId) )
            ORDER BY COALESCE(l.lastChecked, :epoch) ASC, l.id ASC
            """)
    List<LinkEntity> findLinksToCheck(
            Instant epoch, Instant checkedBefore, Instant cursorLastChecked, long cursorId, Pageable pageable);

    @Query("SELECT l.id AS linkId, c.id AS chatId FROM LinkEntity l JOIN l.chats c WHERE l.id IN :linkIds")
    List<LinkChatProjection> findLinkChatPairs(Collection<Long> linkIds);

    @Modifying
    @Query("UPDATE LinkEntity l SET l.lastChecked = :lastChecked WHERE l.id IN :ids")
    void updateLastChecked(Collection<Long> ids, Instant lastChecked);
}
