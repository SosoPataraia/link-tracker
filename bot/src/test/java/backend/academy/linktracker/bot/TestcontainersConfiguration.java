package backend.academy.linktracker.bot;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
        return registry -> registry.add(
            "spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }
}
