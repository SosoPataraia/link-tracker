package backend.academy.linktracker.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai-agent")
public record AiAgentProperties(
        @Valid @NotNull Filtering filtering,
        @Valid @NotNull Summarization summarization,
        @Valid @NotNull AiApi aiApi,
        @Valid @NotNull Prioritization prioritization,
        @Valid @NotNull Grouping grouping) {

    public record Filtering(
            @NotNull List<String> stopWords,
            @NotNull List<String> excludedAuthors,
            @Positive int minLength) {}

    public record Summarization(
            @Positive int threshold, @NotBlank String mode) {}

    public record AiApi(@NotBlank String url, @NotNull String token) {}

    public record Prioritization(
            @NotNull List<String> highKeywords, @NotNull List<String> lowKeywords) {}

    public record Grouping(@Positive long windowMs) {}
}
