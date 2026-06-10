package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.notification.transport", havingValue = "outbox")
@RequiredArgsConstructor
public class OutboxPoller {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, LinkUpdateEvent> avroKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void poll() {
        List<OutboxEvent> pending = outboxRepository.claimPending(100);
        if (pending.isEmpty()) return;

        log.atDebug().addKeyValue("count", pending.size()).log("outbox.poll.found");

        for (OutboxEvent event : pending) {
            try {
                LinkUpdate update = objectMapper.readValue(event.getPayload(), LinkUpdate.class);

                LinkUpdateEvent avroEvent = LinkUpdateEvent.newBuilder()
                        .setId(update.getId())
                        .setUrl(update.getUrl())
                        .setDescription(update.getDescription())
                        .setTgChatIds(update.getTgChatIds())
                        .build();

                avroKafkaTemplate
                        .send(event.getTopic(), event.getKey(), avroEvent)
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                outboxRepository.markProcessed(event.getId());
                log.atInfo()
                        .addKeyValue("id", event.getId())
                        .addKeyValue("url", update.getUrl())
                        .log("outbox.event.processed");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.atError()
                        .addKeyValue("id", event.getId())
                        .addKeyValue("error", e.getMessage())
                        .log("outbox.event.interrupted");
                outboxRepository.markFailed(event.getId());
            } catch (TimeoutException e) {
                log.atError()
                        .addKeyValue("id", event.getId())
                        .addKeyValue("error", e.getMessage())
                        .log("outbox.event.timeout");
                outboxRepository.markFailed(event.getId());
            } catch (Exception e) {
                log.atError()
                        .addKeyValue("id", event.getId())
                        .addKeyValue("error", e.getMessage())
                        .log("outbox.event.failed");
                outboxRepository.markFailed(event.getId());
            }
        }
    }
}
