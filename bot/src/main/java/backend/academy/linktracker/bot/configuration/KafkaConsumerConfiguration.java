package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.kafka.ValidationException;
import backend.academy.linktracker.bot.properties.KafkaProperties;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.lang.Nullable;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfiguration {

    private final KafkaProperties kafkaProperties;

    @Value("${app.kafka.topic.link-updates-dlt:link-updates.DLT}")
    private String linkUpdatesDltTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:bot-group}")
    private String groupId;

    @Value("${app.kafka.schema-registry-url:http://localhost:8085}")
    private String schemaRegistryUrl;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
            @Autowired(required = false) @Nullable SchemaRegistryClient schemaRegistryClient) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        if (schemaRegistryClient != null) {
            var avroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient);
            avroDeserializer.configure(props, false);
            var errorHandlingDeserializer = new ErrorHandlingDeserializer<>(avroDeserializer);
            return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
        }

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, byte[]> dltKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public org.apache.kafka.clients.admin.NewTopic linkUpdatesDltTopic() {
        return org.springframework.kafka.config.TopicBuilder.name(linkUpdatesDltTopic)
                .partitions(3)
                .replicas(3)
                .config("retention.ms", "2592000000")
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory, KafkaTemplate<String, byte[]> dltKafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);

        int retryAttempts = kafkaProperties.getConsumer().getRetryAttempts();

        var recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate, (record, ex) -> {
            log.atError()
                    .addKeyValue("retryAttempts", retryAttempts)
                    .addKeyValue("topic", record.topic())
                    .addKeyValue("key", record.key())
                    .addKeyValue("error", ex.getMessage())
                    .log("kafka.dlt.published");
            return new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", record.partition());
        });

        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, retryAttempts));

        errorHandler.addNotRetryableExceptions(DeserializationException.class);
        errorHandler.addNotRetryableExceptions(ValidationException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
