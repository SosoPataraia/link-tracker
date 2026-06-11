package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ResilientHttpNotificationSender implements NotificationSender {

    private static final String CIRCUIT_BREAKER_NAME = "botClient";

    private final BotClient botClient;
    private final KafkaNotificationSender kafkaFallback;

    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "sendViaKafka")
    public void send(LinkUpdate update) {
        log.atInfo()
                .addKeyValue("url", update.getUrl())
                .addKeyValue("transport", "http")
                .log("notification.send");
        botClient.sendUpdate(update);
    }

    @SuppressWarnings("unused")
    private void sendViaKafka(LinkUpdate update, Exception cause) {
        log.atWarn()
                .addKeyValue("url", update.getUrl())
                .addKeyValue("reason", cause.getMessage())
                .log("notification.http.failed.fallback.kafka");
        kafkaFallback.send(update);
    }
}
