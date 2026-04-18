package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.sender.HttpNotificationSender;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.notification.transport=http"})
class HttpNotificationSenderTest {

    @Autowired
    NotificationSender notificationSender;

    @Test
    void notificationSender_isHttpImplementation() {
        assertThat(notificationSender).isInstanceOf(HttpNotificationSender.class);
    }
}
