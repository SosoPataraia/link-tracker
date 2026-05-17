package backend.academy.linktracker.scrapper;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
public class ResilienceTestcontainersConfiguration {

    @Bean
    DynamicPropertyRegistrar kafkaProperties() {
        return registry -> {
            registry.add("spring.kafka.bootstrap-servers", () -> "localhost:29092");
            registry.add("app.kafka.schema-registry-url", () -> "mock://test");
        };
    }
}
