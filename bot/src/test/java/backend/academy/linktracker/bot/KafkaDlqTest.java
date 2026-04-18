package backend.academy.linktracker.bot;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.kafka.consumer.group-id=bot-group-dlq-test")
class KafkaDlqTest {

    @Autowired
    KafkaTemplate<String, String> dltKafkaTemplate;

    @MockitoBean
    UpdateNotificationHandler notificationHandler;

    @MockitoBean
    TelegramBot telegramBot;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processingError_retriesAndSendsToDlt() throws Exception {
        doThrow(new RuntimeException("Simulated processing error"))
            .when(notificationHandler).handleUpdate(any());

        var update = new LinkUpdate(1L, "https://github.com/user/repo", "Test", List.of(100L));
        dltKafkaTemplate.send("link-updates", objectMapper.writeValueAsString(update));

        await().atMost(Duration.ofSeconds(60))
            .untilAsserted(() -> verify(notificationHandler, times(3)).handleUpdate(any()));
    }

    @Test
    void invalidJson_doesNotCallHandler() throws Exception {
        dltKafkaTemplate.send("link-updates", "this is not valid json {{{");

        await().atMost(Duration.ofSeconds(15))
            .during(Duration.ofSeconds(5))
            .untilAsserted(() -> verify(notificationHandler, never()).handleUpdate(any()));
    }
}
