package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.sender.HttpNotificationSender;
import backend.academy.linktracker.scrapper.sender.KafkaNotificationSender;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import backend.academy.linktracker.scrapper.outbox.OutboxRepository;
import backend.academy.linktracker.scrapper.sender.OutboxNotificationSender;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class NotificationSenderConfiguration {

    @Value("${app.kafka.topic.link-updates:link-updates}")
    private String linkUpdatesTopic;

    @Value("${app.kafka.topic.link-updates-dlt:link-updates.DLT}")
    private String linkUpdatesDltTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Value("${app.kafka.schema-registry-url:http://localhost:8085}")
    private String schemaRegistryUrl;

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public NewTopic linkUpdatesTopic() {
        return TopicBuilder.name(linkUpdatesTopic)
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "604800000")
            .config("min.insync.replicas", "2")
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public NewTopic linkUpdatesDltTopic() {
        return TopicBuilder.name(linkUpdatesDltTopic)
            .partitions(3)
            .replicas(3)
            .config("retention.ms", "2592000000")
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public ProducerFactory<String, LinkUpdateEvent> avroProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public KafkaTemplate<String, LinkUpdateEvent> avroKafkaTemplate(
        ProducerFactory<String, LinkUpdateEvent> avroProducerFactory) {
        return new KafkaTemplate<>(avroProducerFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "http")
    public NotificationSender httpNotificationSender(BotClient botClient) {
        return new HttpNotificationSender(botClient);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "kafka", matchIfMissing = true)
    public NotificationSender kafkaNotificationSender(
        KafkaTemplate<String, LinkUpdateEvent> avroKafkaTemplate) {
        return new KafkaNotificationSender(avroKafkaTemplate, linkUpdatesTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "app.notification.transport", havingValue = "outbox")
    public NotificationSender outboxNotificationSender(
        OutboxRepository outboxRepository,
        ObjectMapper objectMapper) {
        return new OutboxNotificationSender(outboxRepository, objectMapper, linkUpdatesTopic);
    }
}
