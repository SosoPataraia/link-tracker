package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
@TestPropertySource(properties = "app.notification.transport=kafka")
class ScrapperToBotIntegrationTest {

    @Autowired
    LinkCheckerService linkCheckerService;

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    KafkaTemplate<String, ?> kafkaTemplate;

    @MockitoBean
    GitHubClient gitHubClient;

    @MockitoBean
    StackOverflowClient stackOverflowClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM link_tags").update();
        jdbcClient.sql("DELETE FROM link_chat").update();
        jdbcClient.sql("DELETE FROM links").update();
        jdbcClient.sql("DELETE FROM chats").update();
    }

    @Test
    void scrapper_sendsUpdateToKafka_whenNewIssueDetected() {
        chatRepository.register(42L);
        var link = new TrackedLink();
        link.setChatId(42L);
        link.setUrl("https://github.com/user/repo");
        link.setTags(List.of());
        link.setLastChecked(Instant.EPOCH);
        link.setLastUpdated(Instant.now());
        linkRepository.save(link);

        var issue = new IssueItem();
        issue.setTitle("Integration test issue");
        issue.setBody("Test body");
        issue.setCreatedAt(Instant.parse("2024-01-15T10:00:00Z"));
        var user = new IssueItem.UserInfo();
        user.setLogin("testuser");
        issue.setUser(user);

        when(gitHubClient.getNewIssues(anyString(), anyString(), any()))
            .thenReturn(List.of(issue));
        when(gitHubClient.getNewPullRequests(anyString(), anyString(), any()))
            .thenReturn(List.of());

        String bootstrapServers = kafkaTemplate.getProducerFactory()
            .getConfigurationProperties()
            .get(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)
            .toString()
            .replaceAll("[\\[\\]]", "")
            .replace("PLAINTEXT://", "");

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-test-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
            consumer.subscribe(List.of("link-updates"));

            linkCheckerService.checkLinks(linkRepository.findAll());

            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    receivedMessages.add(record.value());
                }
                assertThat(receivedMessages).isNotEmpty();
            });
        }

        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.getFirst()).isNotEmpty();
    }
}
