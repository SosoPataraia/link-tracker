package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ResilienceTestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "app.notification.transport=http",
            "spring.liquibase.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.datasource.url=jdbc:h2:mem:fallbackdb;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
        })
class FallbackNotificationTest {

    @MockitoBean
    BotClient botClient;

    @MockitoBean
    KafkaTemplate<String, LinkUpdateEvent> avroKafkaTemplate;

    @Autowired
    NotificationSender notificationSender;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("botClient").reset();
    }

    @Test
    void whenHttpSucceeds_kafkaNotUsed() {
        var update = new LinkUpdate(1L, "https://github.com/test/repo", "updated", List.of(123L));

        notificationSender.send(update);

        verify(botClient).sendUpdate(update);
        verify(avroKafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void whenCircuitBreakerOpen_fallbackToKafka() {
        doThrow(new RuntimeException("bot is down")).when(botClient).sendUpdate(any());

        var update = new LinkUpdate(1L, "https://github.com/test/repo", "updated", List.of(123L));

        for (int i = 0; i < 5; i++) {
            try {
                notificationSender.send(update);
            } catch (Exception ignored) {
            }
        }

        notificationSender.send(update);

        verify(avroKafkaTemplate, org.mockito.Mockito.atLeastOnce()).send(any(), any(), any());
    }
}
