package backend.academy.linktracker.ai;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    static {
        KAFKA.start();
    }

    @Bean
    KafkaContainer kafkaContainer() {
        return KAFKA;
    }

    @Bean
    public NewTopic rawUpdatesTopic() {
        return new NewTopic("link.raw-updates", 1, (short) 1);
    }

    @Bean
    public NewTopic processedUpdatesTopic() {
        return new NewTopic("link.processed-updates", 1, (short) 1);
    }
}
