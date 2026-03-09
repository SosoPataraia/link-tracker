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
        return RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    @Bean
    public RestClient stackOverflowRestClient(StackoverflowProperties properties) {
        return RestClient.builder()
                .baseUrl("https://api.stackexchange.com/2.3")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient botRestClient(BotProperties properties) {
        return RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }
}
