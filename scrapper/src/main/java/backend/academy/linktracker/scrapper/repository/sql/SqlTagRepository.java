package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;

@RequiredArgsConstructor
public class SqlTagRepository implements TagRepository {

    private final JdbcClient jdbcClient;

    @Override
    public void addTag(long linkId, long chatId, String tag) {
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

    @Override
    public void removeTag(long linkId, long chatId, String tag) {
        jdbcClient
                .sql("""
                DELETE FROM link_tags
                WHERE link_id = :linkId AND chat_id = :chatId AND tag = :tag
                """)
                .param("linkId", linkId)
                .param("chatId", chatId)
                .param("tag", tag)
                .update();
    }

    @Override
    public void removeAllTags(long linkId, long chatId) {
        jdbcClient.sql("""
                DELETE FROM link_tags
                WHERE link_id = :linkId AND chat_id = :chatId
                """).param("linkId", linkId).param("chatId", chatId).update();
    }

    @Override
    public List<String> findTags(long linkId, long chatId) {
        return jdbcClient
                .sql("""
                SELECT tag FROM link_tags
                WHERE link_id = :linkId AND chat_id = :chatId
                """)
                .param("linkId", linkId)
                .param("chatId", chatId)
                .query(String.class)
                .list();
    }

    @Override
    public List<Long> findLinkIdsByTag(long chatId, String tag) {
        return jdbcClient
                .sql("""
                SELECT link_id FROM link_tags
                WHERE chat_id = :chatId AND tag = :tag
                """)
                .param("chatId", chatId)
                .param("tag", tag)
                .query(Long.class)
                .list();
    }
}
