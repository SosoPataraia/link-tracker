package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                    SET last_checked = EXCLUDED.last_checked,
                        last_updated = EXCLUDED.last_updated
                RETURNING id
                """)
                .param("url", link.getUrl())
                .param("lastChecked", toTimestamp(link.getLastChecked()))
                .param("lastUpdated", toTimestamp(link.getLastUpdated()))
                .update(keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to retrieve generated key for url=" + link.getUrl());
        }
        long linkId = key.longValue();
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
        List<Map<String, Object>> rows = jdbcClient
                .sql("""
            SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated,
                   lt.tag
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            LEFT JOIN link_tags lt ON l.id = lt.link_id AND lc.chat_id = lt.chat_id
            WHERE l.id = :id
            """)
                .param("id", id)
                .query((rs, rowNum) -> mapRowToMap(rs))
                .list();
        return buildLinks(rows).stream().findFirst();
    }

    @Override
    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        List<Map<String, Object>> rows = jdbcClient
                .sql("""
                SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated,
                       lt.tag
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                LEFT JOIN link_tags lt ON l.id = lt.link_id AND lc.chat_id = lt.chat_id
                WHERE lc.chat_id = :chatId AND l.url = :url
                """)
                .param("chatId", chatId)
                .param("url", url)
                .query((rs, rowNum) -> mapRowToMap(rs))
                .list();
        return buildLinks(rows).stream().findFirst();
    }

    @Override
    public List<TrackedLink> findAllByChat(long chatId) {
        List<Map<String, Object>> rows = jdbcClient
                .sql("""
            SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated,
                   lt.tag
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            LEFT JOIN link_tags lt ON l.id = lt.link_id AND lc.chat_id = lt.chat_id
            WHERE lc.chat_id = :chatId
            """)
                .param("chatId", chatId)
                .query((rs, rowNum) -> mapRowToMap(rs))
                .list();
        return buildLinks(rows);
    }

    @Override
    public List<TrackedLink> findAllByChat(long chatId, int limit, int offset) {
        List<Map<String, Object>> rows = jdbcClient
                .sql("""
            SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated,
                   lt.tag
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            LEFT JOIN link_tags lt ON l.id = lt.link_id AND lc.chat_id = lt.chat_id
            WHERE lc.chat_id = :chatId
            ORDER BY l.id
            LIMIT :limit OFFSET :offset
            """)
                .param("chatId", chatId)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> mapRowToMap(rs))
                .list();
        return buildLinks(rows);
    }

    @Override
    public Collection<TrackedLink> findAll() {
        List<Map<String, Object>> rows =
                jdbcClient.sql("""
                SELECT l.id, lc.chat_id, l.url, l.last_checked, l.last_updated,
                       lt.tag
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                LEFT JOIN link_tags lt ON l.id = lt.link_id AND lc.chat_id = lt.chat_id
                """).query((rs, rowNum) -> mapRowToMap(rs)).list();
        return buildLinks(rows);
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
        jdbcClient
                .sql("DELETE FROM link_tags WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .update();

        List<Long> orphanedLinkIds =
                jdbcClient.sql("""
                SELECT lc.link_id FROM link_chat lc
                WHERE lc.chat_id = :chatId
                AND NOT EXISTS (
                    SELECT 1 FROM link_chat lc2
                    WHERE lc2.link_id = lc.link_id AND lc2.chat_id != :chatId
                )
                """).param("chatId", chatId).query(Long.class).list();

        jdbcClient
                .sql("DELETE FROM link_chat WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .update();

        if (!orphanedLinkIds.isEmpty()) {
            jdbcClient
                    .sql("DELETE FROM links WHERE id = ANY(:ids)")
                    .param("ids", orphanedLinkIds.toArray(new Long[0]))
                    .update();
        }
    }

    private void saveTags(long linkId, long chatId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (String tag : tags) {
            jdbcClient
                    .sql("""
                    INSERT INTO link_tags (link_id, chat_id, tag)
                    VALUES (:linkId, :chatId, :tag)
                    ON CONFLICT DO NOTHING
                    """)
                    .param("linkId", linkId)
                    .param("chatId", chatId)
                    .param("tag", tag)
                    .update();
        }
    }

    private Map<String, Object> mapRowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("chat_id", rs.getLong("chat_id"));
        row.put("url", rs.getString("url"));
        row.put("last_checked", rs.getTimestamp("last_checked"));
        row.put("last_updated", rs.getTimestamp("last_updated"));
        row.put("tag", rs.getString("tag"));
        return row;
    }

    private List<TrackedLink> buildLinks(List<Map<String, Object>> rows) {
        Map<String, TrackedLink> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            long id = (Long) row.get("id");
            long chatId = (Long) row.get("chat_id");
            String key = id + ":" + chatId;
            TrackedLink link = result.computeIfAbsent(key, k -> {
                var l = new TrackedLink();
                l.setId(id);
                l.setChatId(chatId);
                l.setUrl((String) row.get("url"));
                l.setLastChecked(toInstant((Timestamp) row.get("last_checked")));
                l.setLastUpdated(toInstant((Timestamp) row.get("last_updated")));
                l.setTags(new ArrayList<>());
                return l;
            });
            String tag = (String) row.get("tag");
            if (tag != null) {
                link.getTags().add(tag);
            }
        }
        return new ArrayList<>(result.values());
    }

    private Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
