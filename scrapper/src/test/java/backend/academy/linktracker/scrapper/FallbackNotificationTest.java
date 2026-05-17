package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.sender.KafkaNotificationSender;
import backend.academy.linktracker.scrapper.sender.ResilientHttpNotificationSender;
import java.util.List;
import org.junit.jupiter.api.Test;

class FallbackNotificationTest {

    @Test
    void whenHttpSucceeds_kafkaNotUsed() {
        var botClient = mock(BotClient.class);
        var kafkaSender = mock(KafkaNotificationSender.class);
        var sender = new ResilientHttpNotificationSender(botClient, kafkaSender);

        var update = new LinkUpdate(1L, "https://github.com/test/repo", "updated", List.of(123L));

        sender.send(update);

        verify(botClient).sendUpdate(update);
        verify(kafkaSender, never()).send(any());
    }

    @Test
    void whenHttpFails_kafkaFallbackIsUsed() {
        var botClient = mock(BotClient.class);
        var kafkaSender = mock(KafkaNotificationSender.class);
        var sender = new ResilientHttpNotificationSender(botClient, kafkaSender);

        doThrow(new RuntimeException("bot is down")).when(botClient).sendUpdate(any());

        var update = new LinkUpdate(1L, "https://github.com/test/repo", "updated", List.of(123L));

        sender.sendViaKafka(update, new RuntimeException("bot is down"));

        verify(kafkaSender).send(update);
    }
}
