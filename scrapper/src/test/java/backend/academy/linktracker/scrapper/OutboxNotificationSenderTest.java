package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.outbox.OutboxEvent;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import backend.academy.linktracker.scrapper.sender.OutboxNotificationSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.notification.transport=outbox")
class OutboxNotificationSenderTest {

    @Autowired
    NotificationSender notificationSender;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    JdbcClient jdbcClient;

    @MockitoBean
    KafkaTemplate<String, backend.academy.linktracker.avro.LinkUpdateEvent> avroKafkaTemplate;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM outbox_events").update();
    }

    @Test
    void send_savesToOutbox_notToKafka() throws Exception {
        assertThat(notificationSender).isInstanceOf(OutboxNotificationSender.class);

        var update = new LinkUpdate(1L, "https://github.com/user/repo", "Test", List.of(100L));
        notificationSender.send(update);

        List<OutboxEvent> pending = outboxRepository.findPending(10);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().getTopic()).isEqualTo("link-updates");
        assertThat(pending.getFirst().getPayload()).contains("github.com/user/repo");
        assertThat(pending.getFirst().getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);

        verify(avroKafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void outboxPoller_sendsToKafkaAndMarksProcessed() throws Exception {
        var update = new LinkUpdate(1L, "https://github.com/user/repo", "Test", List.of(100L));
        notificationSender.send(update);

        var future = new java.util.concurrent.CompletableFuture<
                org.springframework.kafka.support.SendResult<
                        String, backend.academy.linktracker.avro.LinkUpdateEvent>>();
        future.complete(null);
        org.mockito.Mockito.when(avroKafkaTemplate.send(any(), any(), any())).thenReturn(future);

        var poller = new backend.academy.linktracker.scrapper.outbox.OutboxPoller(
                outboxRepository, avroKafkaTemplate, new ObjectMapper());
        poller.poll();

        List<OutboxEvent> pending = outboxRepository.findPending(10);
        assertThat(pending).isEmpty();

        verify(avroKafkaTemplate).send(any(), any(), any());
    }

    @Test
    void outboxPoller_onKafkaFailure_marksAsFailed() throws Exception {
        var update = new LinkUpdate(1L, "https://github.com/user/repo", "Test", List.of(100L));
        notificationSender.send(update);

        org.mockito.Mockito.when(avroKafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        var poller = new backend.academy.linktracker.scrapper.outbox.OutboxPoller(
                outboxRepository, avroKafkaTemplate, new ObjectMapper());
        poller.poll();

        List<OutboxEvent> pending = outboxRepository.findPending(10);
        assertThat(pending).isEmpty();

        Long failedCount = jdbcClient
                .sql("SELECT COUNT(*) FROM outbox_events WHERE status = 'FAILED'")
                .query(Long.class)
                .single();
        assertThat(failedCount).isEqualTo(1L);
    }
}
