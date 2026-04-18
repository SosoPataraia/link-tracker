package backend.academy.linktracker.scrapper.outbox;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    private Long id;
    private String topic;
    private String key;
    private String payload;
    private OutboxStatus status;
    private Instant createdAt;
    private Instant processedAt;

    public enum OutboxStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}
