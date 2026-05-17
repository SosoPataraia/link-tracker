package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.BotProperties;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.HttpClientProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfiguration {

    private HttpComponentsClientHttpRequestFactory requestFactory(HttpClientProperties props) {
        var requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(props.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(props.getReadTimeout()))
                .build();

        var httpClient =
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    @Bean
    public RestClient gitHubRestClient(GithubProperties properties, HttpClientProperties httpClientProperties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(httpClientProperties))
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getToken());
        }
        return builder.build();
    }

    @Bean
    public RestClient stackOverflowRestClient(
            StackoverflowProperties properties, HttpClientProperties httpClientProperties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(httpClientProperties))
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public RestClient botRestClient(BotProperties properties, HttpClientProperties httpClientProperties) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(httpClientProperties))
                .build();
    }
}
