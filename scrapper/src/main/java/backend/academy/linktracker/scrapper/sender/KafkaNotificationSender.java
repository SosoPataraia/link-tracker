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
            .setAuthor(null)
            .setTgChatIds(update.getTgChatIds())
            .build();
        log.atInfo()
            .addKeyValue("url", update.getUrl())
            .addKeyValue("topic", topicName)
            .log("kafka.notification.sending");
        kafkaTemplate.send(topicName, String.valueOf(update.getId()), event);
    }
}
