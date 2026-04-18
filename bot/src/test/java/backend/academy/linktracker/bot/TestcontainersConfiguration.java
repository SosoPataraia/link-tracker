package backend.academy.linktracker.bot;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import com.redis.testcontainers.RedisContainer;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    // Uncomment to start PostgreSQLContainer
    // @Bean
    // @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
    }

    // Uncomment to start RedisContainer
    // @Bean
    // @ServiceConnection
    RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:8.2-alpine"));
    }

    @Bean
    KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
    }

    @Bean
    DynamicPropertyRegistrar kafkaProperties(KafkaContainer kafkaContainer) {
        return registry -> {
            registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
            registry.add("app.kafka.schema-registry-url", () -> "mock://test");
        };
    }

    @Bean
    SchemaRegistryClient mockSchemaRegistryClient() throws Exception {
        var client = new MockSchemaRegistryClient();
        client.register("link-updates-value",
            new AvroSchema(LinkUpdateEvent.getClassSchema()));
        return client;
    }

    @Bean
    @Primary
    KafkaTemplate<String, String> testStringKafkaTemplate(KafkaContainer kafkaContainer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    KafkaTemplate<String, LinkUpdateEvent> avroTestKafkaTemplate(
        KafkaContainer kafkaContainer,
        SchemaRegistryClient schemaRegistryClient) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put("schema.registry.url", "mock://test");

        var serializer = new KafkaAvroSerializer(schemaRegistryClient);
        serializer.configure(props, false);

        var factory = new DefaultKafkaProducerFactory<String, LinkUpdateEvent>(props);
        factory.setValueSerializer((topic, data) ->
            serializer.serialize(topic, data));
        return new KafkaTemplate<>(factory);
    }
}
