package backend.academy.linktracker.scrapper.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private final JdbcClient jdbcClient;

    @Override
    public void save(OutboxEvent event) {
        jdbcClient.sql("""
                INSERT INTO outbox_events (topic, key, payload, status)
                VALUES (:topic, :key, :payload, :status)
                """)
            .param("topic", event.getTopic())
            .param("key", event.getKey())
            .param("payload", event.getPayload())
            .param("status", OutboxEvent.OutboxStatus.PENDING.name())
            .update();
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return jdbcClient.sql("""
                SELECT id, topic, key, payload, status, created_at, processed_at
                FROM outbox_events
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT :limit
                """)
            .param("limit", limit)
            .query((rs, rowNum) -> mapRow(rs))
            .list();
    }

    @Override
    public void markProcessed(long id) {
        jdbcClient.sql("""
                UPDATE outbox_events
                SET status = 'PROCESSED', processed_at = :now
                WHERE id = :id
                """)
            .param("now", Timestamp.from(Instant.now()))
            .param("id", id)
            .update();
    }

    @Override
    public void markFailed(long id) {
        jdbcClient.sql("""
                UPDATE outbox_events
                SET status = 'FAILED', processed_at = :now
                WHERE id = :id
                """)
            .param("now", Timestamp.from(Instant.now()))
            .param("id", id)
            .update();
    }

    private OutboxEvent mapRow(ResultSet rs) throws SQLException {
        var event = new OutboxEvent();
        event.setId(rs.getLong("id"));
        event.setTopic(rs.getString("topic"));
        event.setKey(rs.getString("key"));
        event.setPayload(rs.getString("payload"));
        event.setStatus(OutboxEvent.OutboxStatus.valueOf(rs.getString("status")));
        var createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) event.setCreatedAt(createdAt.toInstant());
        var processedAt = rs.getTimestamp("processed_at");
        if (processedAt != null) event.setProcessedAt(processedAt.toInstant());
        return event;
    }
}
