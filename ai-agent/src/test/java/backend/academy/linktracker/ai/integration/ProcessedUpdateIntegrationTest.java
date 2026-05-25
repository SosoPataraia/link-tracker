package backend.academy.linktracker.ai.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.ai.TestcontainersConfiguration;
import backend.academy.linktracker.avro.Priority;
import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import backend.academy.linktracker.avro.RawUpdateEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.kafka.schema-registry.url=mock://test",
            "ai-agent.summarization.mode=stub",
            "ai-agent.grouping.window-ms=500"
        })
class ProcessedUpdateIntegrationTest {

    private static final String SCHEMA_REGISTRY_URL = "mock://test";
    private static final String RAW_TOPIC = "link.raw-updates";
    private static final String PROCESSED_TOPIC = "link.processed-updates";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", TestcontainersConfiguration.KAFKA::getBootstrapServers);
    }

    @Autowired
    @Qualifier("rawKafkaTemplate")
    private KafkaTemplate<String, RawUpdateEvent> rawKafkaTemplate;

    @Test
    void whenValidMessagePublished_thenProcessedMessageAppearsWithPriority() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
                .setId(100L)
                .setDescription("A critical security vulnerability was found in the login flow")
                .setAuthor("normal-user")
                .setTgChatIds(List.of(111L, 222L))
                .build();

        rawKafkaTemplate.send(RAW_TOPIC, "100", event);

        List<ProcessedUpdateEvent> received = new CopyOnWriteArrayList<>();

        try (var consumer = buildProcessedConsumer()) {
            consumer.subscribe(Collections.singletonList(PROCESSED_TOPIC));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                ConsumerRecords<String, ProcessedUpdateEvent> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> received.add(r.value()));
                assertThat(received).isNotEmpty();
                assertThat(received.getFirst().getId()).isEqualTo(100L);
                assertThat(received.getFirst().getPriority()).isEqualTo(Priority.HIGH);
            });
        }
    }

    @Test
    void whenValidMessageWithNoKeywords_thenPublishedWithMediumPriority() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
                .setId(200L)
                .setDescription("Updated the user profile page with a new design and better layout")
                .setAuthor("normal-user")
                .setTgChatIds(List.of(333L))
                .build();

        rawKafkaTemplate.send(RAW_TOPIC, "200", event);

        List<ProcessedUpdateEvent> received = new CopyOnWriteArrayList<>();

        try (var consumer = buildProcessedConsumer()) {
            consumer.subscribe(Collections.singletonList(PROCESSED_TOPIC));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                ConsumerRecords<String, ProcessedUpdateEvent> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> received.add(r.value()));
                assertThat(received).isNotEmpty();
                // NORMAL = MEDIUM in Avro schema
                assertThat(received.getFirst().getPriority()).isEqualTo(Priority.NORMAL);
            });
        }
    }

    @Test
    void whenFilteredMessagePublished_thenNotAppearsOnProcessedTopic() {
        RawUpdateEvent filteredEvent = RawUpdateEvent.newBuilder()
                .setId(999L)
                .setDescription("This is a spam message that should be filtered out completely")
                .setAuthor("normal-user")
                .setTgChatIds(List.of(444L))
                .build();

        rawKafkaTemplate.send(RAW_TOPIC, "999", filteredEvent);

        List<ProcessedUpdateEvent> received = new CopyOnWriteArrayList<>();

        try (var consumer = buildProcessedConsumer()) {
            consumer.subscribe(Collections.singletonList(PROCESSED_TOPIC));
            await().atMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(5)).untilAsserted(() -> {
                ConsumerRecords<String, ProcessedUpdateEvent> records = consumer.poll(Duration.ofMillis(200));
                records.forEach(r -> {
                    assertThat(r.value().getId()).isNotEqualTo(999L);
                    received.add(r.value());
                });
            });
        }
    }

    @Test
    void whenTwoUpdatesForSameChatId_thenGroupedIntoOneMessage() {
        long chatId = 555L;

        RawUpdateEvent event1 = RawUpdateEvent.newBuilder()
                .setId(300L)
                .setDescription("First update with sufficient length to pass all filters here")
                .setAuthor("normal-user")
                .setTgChatIds(List.of(chatId))
                .build();

        RawUpdateEvent event2 = RawUpdateEvent.newBuilder()
                .setId(301L)
                .setDescription("Second update with sufficient length to pass all filters here")
                .setAuthor("normal-user")
                .setTgChatIds(List.of(chatId))
                .build();

        rawKafkaTemplate.send(RAW_TOPIC, "300", event1);
        rawKafkaTemplate.send(RAW_TOPIC, "301", event2);

        List<ProcessedUpdateEvent> received = new CopyOnWriteArrayList<>();

        try (var consumer = buildProcessedConsumer()) {
            consumer.subscribe(Collections.singletonList(PROCESSED_TOPIC));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                ConsumerRecords<String, ProcessedUpdateEvent> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> {
                    if (r.value().getTgChatIds().contains(chatId)) {
                        received.add(r.value());
                    }
                });
                assertThat(received).hasSize(1);
                assertThat(received.getFirst().getDescription().toString()).contains("1.");
                assertThat(received.getFirst().getDescription().toString()).contains("2.");
            });
        }
    }

    private KafkaConsumer<String, ProcessedUpdateEvent> buildProcessedConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                TestcontainersConfiguration.KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-group-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                KafkaAvroDeserializer.class,
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
                SCHEMA_REGISTRY_URL,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG,
                true));
    }
}
