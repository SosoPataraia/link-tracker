package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.BotProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfiguration {

    @Bean
    public RestClient gitHubRestClient(GithubProperties properties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getToken());
        }
        return builder.build();
    }

    @Bean
    public RestClient stackOverflowRestClient(StackoverflowProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient botRestClient(BotProperties properties) {
        return RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }
}
