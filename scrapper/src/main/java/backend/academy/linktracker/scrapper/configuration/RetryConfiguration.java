package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.resilience.RetryableStatusPredicate;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfiguration {

    @Bean
    public RetryConfigCustomizer githubRetryCustomizer(RetryableStatusPredicate predicate) {
        return RetryConfigCustomizer.of("githubClient", builder -> builder.retryOnException(predicate));
    }

    @Bean
    public RetryConfigCustomizer stackOverflowRetryCustomizer(RetryableStatusPredicate predicate) {
        return RetryConfigCustomizer.of("stackOverflowClient", builder -> builder.retryOnException(predicate));
    }

    @Bean
    public RetryConfigCustomizer botRetryCustomizer(RetryableStatusPredicate predicate) {
        return RetryConfigCustomizer.of("botClient", builder -> builder.retryOnException(predicate));
    }
}
