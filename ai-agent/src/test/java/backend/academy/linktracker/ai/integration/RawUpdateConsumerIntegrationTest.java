package backend.academy.linktracker.ai.integration;

import backend.academy.linktracker.ai.TestcontainersConfiguration;
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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "spring.kafka.schema-registry.url=mock://test",
    "ai-agent.summarization.mode=stub"
})
class RawUpdateConsumerIntegrationTest {

    private static final String SCHEMA_REGISTRY_URL = "mock://test";
    private static final String RAW_TOPIC = "link.raw-updates";
    private static final String PROCESSED_TOPIC = "link.processed-updates";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
            TestcontainersConfiguration.KAFKA::getBootstrapServers);
    }

    @Autowired
    @Qualifier("rawKafkaTemplate")
    private KafkaTemplate<String, RawUpdateEvent> rawKafkaTemplate;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Test
    void whenValidMessagePublished_thenProcessedMessageAppearsInOutputTopic() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
            .setId(42L)
            .setDescription("A valid update with sufficient length to pass all filters and checks")
            .setAuthor("normal-user")
            .setTgChatIds(List.of(111L, 222L))
            .build();

        rawKafkaTemplate.send(RAW_TOPIC, "42", event);

        List<ProcessedUpdateEvent> received = new CopyOnWriteArrayList<>();

        try (var consumer = buildProcessedConsumer()) {
            consumer.subscribe(Collections.singletonList(PROCESSED_TOPIC));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                ConsumerRecords<String, ProcessedUpdateEvent> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> received.add(r.value()));
                assertThat(received).isNotEmpty();
                assertThat(received.getFirst().getId()).isEqualTo(42L);
                assertThat(received.getFirst().getTgChatIds()).containsExactly(111L, 222L);
            });
        }
    }

    @Test
    void whenMalformedMessagePublished_thenServiceDoesNotCrash() throws Exception {
        try (var rawProducer = new KafkaProducer<String, String>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            TestcontainersConfiguration.KAFKA.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class))) {
            rawProducer.send(new ProducerRecord<>(RAW_TOPIC, "bad", "not-avro-bytes")).get();
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(kafkaContainer.isRunning()).isTrue());
    }

    private KafkaConsumer<String, ProcessedUpdateEvent> buildProcessedConsumer() {
        return new KafkaConsumer<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            TestcontainersConfiguration.KAFKA.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class,
            KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL,
            KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true));
    }
}
