package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.sender.KafkaNotificationSender;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
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
    KafkaTemplate<String, LinkUpdateEvent> avroKafkaTemplate;

    @Test
    void notificationSender_isKafkaImplementation() {
        assertThat(notificationSender).isInstanceOf(KafkaNotificationSender.class);
    }

    @Test
    void send_doesNotThrow() {
        var update = new LinkUpdate(1L, "https://github.com/user/repo", "Test update", List.of(100L));
        notificationSender.send(update);
    }
}
