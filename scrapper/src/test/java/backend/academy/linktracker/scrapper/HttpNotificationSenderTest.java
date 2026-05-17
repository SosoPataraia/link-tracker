package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.sender.NotificationSender;
import backend.academy.linktracker.scrapper.sender.ResilientHttpNotificationSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.liquibase.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
            "spring.datasource.url=jdbc:h2:mem:httpnotificationdb;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "app.notification.transport=http"
        })
@Import(ResilienceTestcontainersConfiguration.class)
@ActiveProfiles("test")
class HttpNotificationSenderTest {

    @Autowired
    NotificationSender notificationSender;

    @Test
    void notificationSender_isHttpImplementation() {
        assertThat(notificationSender).isInstanceOf(ResilientHttpNotificationSender.class);
    }
}
