package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, LinkUpdateEvent> avroKafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    public void poll() {
        List<OutboxEvent> pending = outboxRepository.findPending(100);
        if (pending.isEmpty()) return;

        log.debug("Polling outbox: found {} pending events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                LinkUpdate update = objectMapper.readValue(event.getPayload(), LinkUpdate.class);

                LinkUpdateEvent avroEvent = LinkUpdateEvent.newBuilder()
                    .setId(update.getId())
                    .setUrl(update.getUrl())
                    .setDescription(update.getDescription())
                    .setTgChatIds(update.getTgChatIds())
                    .build();

                avroKafkaTemplate.send(event.getTopic(), event.getKey(), avroEvent).get();
                outboxRepository.markProcessed(event.getId());
                log.info("Outbox event processed: id={} url={}", event.getId(), update.getUrl());
            } catch (Exception e) {
                log.error("Failed to process outbox event id={}: {}", event.getId(), e.getMessage());
                outboxRepository.markFailed(event.getId());
            }
        }
    }
}
