package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.properties.KafkaProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
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
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfiguration {

    private final KafkaProperties kafkaProperties;

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:bot-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, LinkUpdate> consumerFactory() {
        var deserializer = new JacksonJsonDeserializer<>(LinkUpdate.class);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public KafkaTemplate<String, String> dltKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LinkUpdate> kafkaListenerContainerFactory(
        ConsumerFactory<String, LinkUpdate> consumerFactory,
        KafkaTemplate<String, String> dltKafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, LinkUpdate>();
        factory.setConsumerFactory(consumerFactory);

        int retryAttempts = kafkaProperties.getConsumer().getRetryAttempts();

        var recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
            (record, ex) -> {
                log.error("Message sent to DLT after {} retries. topic={} key={} error={}",
                    retryAttempts, record.topic(), record.key(), ex.getMessage());
                return new org.apache.kafka.common.TopicPartition(
                    record.topic() + ".DLT", record.partition());
            });

        var errorHandler = new DefaultErrorHandler(recoverer,
            new FixedBackOff(1000L, retryAttempts - 1L));

        errorHandler.addNotRetryableExceptions(
            org.springframework.kafka.support.serializer.DeserializationException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
