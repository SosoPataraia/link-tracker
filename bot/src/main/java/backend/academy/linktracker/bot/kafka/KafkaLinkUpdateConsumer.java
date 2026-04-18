package backend.academy.linktracker.bot.kafka;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaLinkUpdateConsumer {

    private final UpdateNotificationHandler notificationHandler;

    @KafkaListener(
        topics = "${app.kafka.topic.link-updates:link-updates}",
        groupId = "${spring.kafka.consumer.group-id:bot-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(LinkUpdateEvent event) {
        log.atInfo()
            .addKeyValue("url", event.getUrl())
            .addKeyValue("chatIds", event.getTgChatIds())
            .log("kafka.update.received");

        var update = new LinkUpdate(
            event.getId(),
            event.getUrl().toString(),
            event.getDescription() != null ? event.getDescription().toString() : null,
            new ArrayList<>(event.getTgChatIds())
        );
        notificationHandler.handleUpdate(update);
    }
}
