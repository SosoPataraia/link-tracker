package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class KafkaNotificationSender implements NotificationSender {

    private final KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate;
    private final String topicName;

    @Override
    public void send(LinkUpdate update) {
        var event = LinkUpdateEvent.newBuilder()
                .setId(update.getId())
                .setUrl(update.getUrl())
                .setDescription(update.getDescription())
                .setTgChatIds(update.getTgChatIds())
                .build();

        log.info("Sending update via Kafka (Avro): url={} topic={}", update.getUrl(), topicName);
        kafkaTemplate.send(topicName, String.valueOf(update.getId()), event);
    }
}
