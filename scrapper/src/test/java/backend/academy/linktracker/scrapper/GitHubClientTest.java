package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GitHubClientImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class GitHubClientTest {

    @Test
    void getLastUpdated_parsesDateCorrectly(@InjectWireMock WireMockServer wireMock) {
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

        var client = new GitHubClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        Instant result = client.getLastUpdated("user", "repo");
        assertThat(result).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
    }

    @Test
    void getLastUpdated_handlesErrorGracefully(@InjectWireMock WireMockServer wireMock) {
        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        var client = new GitHubClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        assertThat(client.getLastUpdated("user", "repo")).isNull();
    }

    @Test
    void getIssuesAndPullRequests_throwsOnError(@InjectWireMock WireMockServer wireMock) {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse().withStatus(503)));

        var client = new GitHubClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        assertThatThrownBy(() -> client.getIssuesAndPullRequests("user", "repo", Instant.EPOCH))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void getIssuesAndPullRequests_splitsByPullRequestField(@InjectWireMock WireMockServer wireMock) {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                  { "number": 1, "title": "Issue", "created_at": "2024-01-16T00:00:00Z",
                                    "user": { "login": "alice" } },
                                  { "number": 2, "title": "PR",    "created_at": "2024-01-16T00:00:00Z",
                                    "pull_request": {}, "user": { "login": "bob" } }
                                ]
                                """)));

        var client = new GitHubClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        var items = client.getIssuesAndPullRequests("user", "repo", Instant.EPOCH);
        assertThat(items).hasSize(2);
        assertThat(items.stream().filter(i -> !i.isPullRequest()).toList()).hasSize(1);
        assertThat(items.stream().filter(IssueItem -> IssueItem.isPullRequest()).toList())
                .hasSize(1);
    }
}
