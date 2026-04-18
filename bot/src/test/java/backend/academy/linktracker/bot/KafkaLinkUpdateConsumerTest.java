package backend.academy.linktracker.bot;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.handler.UpdateNotificationHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    KafkaTemplate<String, String> dltKafkaTemplate;

    @MockitoBean
    UpdateNotificationHandler notificationHandler;

    @MockitoBean
    TelegramBot telegramBot;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validMessage_isConsumedAndHandled() throws Exception {
        var update = new LinkUpdate(1L, "https://github.com/user/repo", "New issue", List.of(100L, 200L));
        String json = objectMapper.writeValueAsString(update);

        dltKafkaTemplate.send("link-updates", json);

        await().atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> verify(notificationHandler).handleUpdate(any(LinkUpdate.class)));
    }
}
