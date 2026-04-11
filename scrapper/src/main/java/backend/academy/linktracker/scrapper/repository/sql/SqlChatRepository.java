package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;

@RequiredArgsConstructor
public class SqlChatRepository implements ChatRepository {

    private final JdbcClient jdbcClient;

    @Override
    public void register(long chatId) {
        jdbcClient
                .sql("INSERT INTO chats (id) VALUES (:id) ON CONFLICT DO NOTHING")
                .param("id", chatId)
                .update();
    }

    @Override
    public boolean exists(long chatId) {
        return jdbcClient
                        .sql("SELECT COUNT(*) FROM chats WHERE id = :id")
                        .param("id", chatId)
                        .query(Long.class)
                        .single()
                > 0;
    }

    @Override
    public void remove(long chatId) {
        jdbcClient.sql("DELETE FROM chats WHERE id = :id").param("id", chatId).update();
    }
}
