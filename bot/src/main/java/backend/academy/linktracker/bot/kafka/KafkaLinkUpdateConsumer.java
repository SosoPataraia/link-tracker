package backend.academy.linktracker.bot.kafka;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
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
    public void consume(LinkUpdate update) {
        log.atInfo()
            .addKeyValue("url", update.getUrl())
            .addKeyValue("chatIds", update.getTgChatIds())
            .log("kafka.update.received");
        notificationHandler.handleUpdate(update);
    }
}
