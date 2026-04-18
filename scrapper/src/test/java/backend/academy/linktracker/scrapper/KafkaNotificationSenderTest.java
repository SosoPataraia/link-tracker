package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.sender.KafkaNotificationSender;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.notification.transport=kafka"
})
class KafkaNotificationSenderTest {

    @Autowired
    NotificationSender notificationSender;

    @Autowired
    KafkaTemplate<String, LinkUpdate> kafkaTemplate;

    @Test
    void notificationSender_isKafkaImplementation() {
        assertThat(notificationSender).isInstanceOf(KafkaNotificationSender.class);
    }

    @Test
    void send_doesNotThrow() {
        var update = new LinkUpdate(1L, "https://github.com/user/repo", "Test update", List.of(100L));
        notificationSender.send(update);
        // If we get here without exception, producer is wired correctly
    }
}
