package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.outbox.OutboxEvent;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OutboxNotificationSender implements NotificationSender {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final String topicName;

    @Override
    @SneakyThrows
    public void send(LinkUpdate update) {
        var event = new OutboxEvent();
        event.setTopic(topicName);
        event.setKey(String.valueOf(update.getId()));
        event.setPayload(objectMapper.writeValueAsString(update));

        outboxRepository.save(event);
        log.info("Saved to outbox: url={} topic={}", update.getUrl(), topicName);
    }
}
