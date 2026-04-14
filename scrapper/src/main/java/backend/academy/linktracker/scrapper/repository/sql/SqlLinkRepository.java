package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class SqlLinkRepository implements LinkRepository {

    private final JdbcClient jdbcClient;

    @Override
    @Transactional
    public TrackedLink save(TrackedLink link) {
        var keyHolder = new GeneratedKeyHolder();
        jdbcClient
            .sql("""
                INSERT INTO links (url, last_checked, last_updated)
                VALUES (:url, :lastChecked, :lastUpdated)
                ON CONFLICT (url) DO UPDATE
                    SET last_checked = EXCLUDED.last_checked
                RETURNING id
                """)
            .param("url", link.getUrl())
            .param("lastChecked", toTimestamp(link.getLastChecked()))
            .param("lastUpdated", toTimestamp(link.getLastUpdated()))
            .update(keyHolder);

        long linkId = keyHolder.getKey().longValue();
        link.setId(linkId);

        jdbcClient
            .sql("""
                INSERT INTO link_chat (link_id, chat_id)
                VALUES (:linkId, :chatId)
                ON CONFLICT DO NOTHING
                """)
            .param("linkId", linkId)
            .param("chatId", link.getChatId())
            .update();

        saveTags(linkId, link.getChatId(), link.getTags());

        return link;
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return jdbcClient
            .sql("""
                SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                WHERE l.id = :id
                """)
            .param("id", id)
            .query((rs, rowNum) -> mapRow(rs))
            .optional()
            .map(this::withTags);
    }

    @Override
    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        return jdbcClient
            .sql("""
                SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                WHERE lc.chat_id = :chatId AND l.url = :url
                """)
            .param("chatId", chatId)
            .param("url", url)
            .query((rs, rowNum) -> mapRow(rs))
            .optional()
            .map(this::withTags);
    }

    @Override
    public List<TrackedLink> findAllByChat(long chatId) {
        var links = jdbcClient
            .sql("""
                SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                WHERE lc.chat_id = :chatId
                """)
            .param("chatId", chatId)
            .query((rs, rowNum) -> mapRow(rs))
            .list();
        return links.stream().map(this::withTags).toList();
    }

    @Override
    public Collection<TrackedLink> findAll() {
        var links = jdbcClient.sql("""
                SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                """).query((rs, rowNum) -> mapRow(rs)).list();
        return links.stream().map(this::withTags).toList();
    }

    @Override
    @Transactional
    public boolean remove(long chatId, String url) {
        var linkId = jdbcClient
            .sql("SELECT id FROM links WHERE url = :url")
            .param("url", url)
            .query(Long.class)
            .optional();

        if (linkId.isEmpty()) return false;

        long id = linkId.orElseThrow();

        jdbcClient
            .sql("DELETE FROM link_tags WHERE link_id = :linkId AND chat_id = :chatId")
            .param("linkId", id)
            .param("chatId", chatId)
            .update();

        int deleted = jdbcClient
            .sql("DELETE FROM link_chat WHERE link_id = :linkId AND chat_id = :chatId")
            .param("linkId", id)
            .param("chatId", chatId)
            .update();

        long remaining = jdbcClient
            .sql("SELECT COUNT(*) FROM link_chat WHERE link_id = :linkId")
            .param("linkId", id)
            .query(Long.class)
            .single();

        if (remaining == 0) {
            jdbcClient.sql("DELETE FROM links WHERE id = :id").param("id", id).update();
        }

        return deleted > 0;
    }

    @Override
    @Transactional
    public void removeAllByChat(long chatId) {
        var linkIds = jdbcClient
            .sql("SELECT link_id FROM link_chat WHERE chat_id = :chatId")
            .param("chatId", chatId)
            .query(Long.class)
            .list();

        jdbcClient
            .sql("DELETE FROM link_tags WHERE chat_id = :chatId")
            .param("chatId", chatId)
            .update();

        jdbcClient
            .sql("DELETE FROM link_chat WHERE chat_id = :chatId")
            .param("chatId", chatId)
            .update();

        for (long linkId : linkIds) {
            long remaining = jdbcClient
                .sql("SELECT COUNT(*) FROM link_chat WHERE link_id = :linkId")
                .param("linkId", linkId)
                .query(Long.class)
                .single();
            if (remaining == 0) {
                jdbcClient
                    .sql("DELETE FROM links WHERE id = :id")
                    .param("id", linkId)
                    .update();
            }
        }
    }

    @Override
    public void updateLastChecked(long linkId, Instant lastChecked) {
        jdbcClient
            .sql("UPDATE links SET last_checked = :lastChecked WHERE id = :id")
            .param("lastChecked", toTimestamp(lastChecked))
            .param("id", linkId)
            .update();
    }

    private void saveTags(long linkId, long chatId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (String tag : tags) {
            jdbcClient
                .sql("""
                    INSERT INTO link_tags (link_id, chat_id, tag)
                    VALUES (:linkId, :chatId, :tag)
                    """)
                .param("linkId", linkId)
                .param("chatId", chatId)
                .param("tag", tag)
                .update();
        }
    }

    private List<String> fetchTags(long linkId, long chatId) {
        return jdbcClient
            .sql("SELECT tag FROM link_tags WHERE link_id = :linkId AND chat_id = :chatId")
            .param("linkId", linkId)
            .param("chatId", chatId)
            .query(String.class)
            .list();
    }

    private TrackedLink withTags(TrackedLink link) {
        link.setTags(fetchTags(link.getId(), link.getChatId()));
        return link;
    }

    private TrackedLink mapRow(ResultSet rs) throws SQLException {
        var link = new TrackedLink();
        link.setId(rs.getLong("id"));
        link.setChatId(rs.getLong("chat_id"));
        link.setUrl(rs.getString("url"));
        link.setLastChecked(toInstant(rs.getTimestamp("last_checked")));
        link.setLastUpdated(toInstant(rs.getTimestamp("last_updated")));
        link.setTags(new ArrayList<>());
        return link;
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
