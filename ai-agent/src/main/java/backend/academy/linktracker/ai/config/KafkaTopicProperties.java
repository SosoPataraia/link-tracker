package backend.academy.linktracker.ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.kafka.topic")
public record KafkaTopicProperties(
        @NotBlank String rawUpdates, @NotBlank String processedUpdates) {}
