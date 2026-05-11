package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GitHubClientImpl;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class GitHubClientTest {

    @Test
    void getLastUpdated_parsesDateCorrectly(
        @org.wiremock.spring.InjectWireMock com.github.tomakehurst.wiremock.WireMockServer wireMock) {
        stubFor(get(urlPathEqualTo("/repos/user/repo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                                {
                                  "full_name": "user/repo",
                                  "pushed_at": "2024-01-15T10:30:00Z",
                                  "updated_at": "2024-01-15T10:30:00Z"
                                }
                                """)));

        var restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        var client = new GitHubClientImpl(restClient);

        Instant result = client.getLastUpdated("user", "repo");
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
    }

    @Test
    void getLastUpdated_throwsOnServerError(
        @org.wiremock.spring.InjectWireMock com.github.tomakehurst.wiremock.WireMockServer wireMock) {
        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        var restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        var client = new GitHubClientImpl(restClient);

        assertThatThrownBy(() -> client.getLastUpdated("user", "repo"))
            .isInstanceOf(RestClientException.class);
    }

    @Test
    void getLastUpdated_throwsOn404(
        @org.wiremock.spring.InjectWireMock com.github.tomakehurst.wiremock.WireMockServer wireMock) {
        stubFor(get(urlPathEqualTo("/repos/user/repo"))
            .willReturn(aResponse().withStatus(404).withBody("{\"message\": \"Not Found\"}")));

        var restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        var client = new GitHubClientImpl(restClient);

        assertThatThrownBy(() -> client.getLastUpdated("user", "repo"))
            .isInstanceOf(RestClientException.class);
    }
}
