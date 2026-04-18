package backend.academy.linktracker.bot;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import com.pengrad.telegrambot.TelegramBot;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class KafkaLinkUpdateConsumerTest {

    @Autowired
    KafkaTemplate<String, LinkUpdateEvent> avroTestKafkaTemplate;

    @MockitoBean
    UpdateNotificationHandler notificationHandler;

    @MockitoBean
    TelegramBot telegramBot;

    @Test
    void validMessage_isConsumedAndHandled() {
        var event = LinkUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/user/repo")
                .setDescription("New issue")
                .setTgChatIds(List.of(100L, 200L))
                .build();

        avroTestKafkaTemplate.send("link-updates", event);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> verify(notificationHandler).handleUpdate(any(LinkUpdate.class)));
    }
}
