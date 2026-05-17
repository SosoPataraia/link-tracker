package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GitHubClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class ResilienceTest {

    @InjectWireMock
    WireMockServer wireMock;

    @Autowired
    GitHubClient gitHubClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        // will be overridden per-test via WireMock injection, but base URL needed for RestClient bean
    }

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("githubClient").reset();
    }

    // TC-1.1: Timeout
    @Test
    void getLastUpdated_timesOutWhenServiceIsSlow() {
        stubFor(get(urlPathEqualTo("/repos/user/repo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"full_name":"user/repo","pushed_at":"2024-01-15T10:30:00Z","updated_at":"2024-01-15T10:30:00Z"}
                                """)
                        .withFixedDelay(10000)));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> gitHubClient.getLastUpdated("user", "repo")).isInstanceOf(Exception.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(28000);
    }

    @Test
    void getLastUpdated_retriesOn5xxThenSucceeds() {
        stubFor(get(urlPathEqualTo("/repos/user/repo"))
                .inScenario("retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("first-failure"));

        stubFor(get(urlPathEqualTo("/repos/user/repo"))
                .inScenario("retry")
                .whenScenarioStateIs("first-failure")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second-failure"));

        stubFor(get(urlPathEqualTo("/repos/user/repo"))
                .inScenario("retry")
                .whenScenarioStateIs("second-failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"full_name":"user/repo","pushed_at":"2024-01-15T10:30:00Z","updated_at":"2024-01-15T10:30:00Z"}
                                """)));

        Instant result = gitHubClient.getLastUpdated("user", "repo");

        assertThat(result).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
        verify(3, getRequestedFor(urlPathEqualTo("/repos/user/repo")));
    }

    @Test
    void getLastUpdated_doesNotRetryOn4xx() {
        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> gitHubClient.getLastUpdated("user", "repo")).isInstanceOf(Exception.class);

        verify(1, getRequestedFor(urlPathEqualTo("/repos/user/repo")));
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold() {
        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("githubClient");

        for (int i = 0; i < 5; i++) {
            try {
                gitHubClient.getLastUpdated("user", "repo");
            } catch (Exception ignored) {
            }
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_openStateRejectsImmediately() {
        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("githubClient");

        for (int i = 0; i < 5; i++) {
            try {
                gitHubClient.getLastUpdated("user", "repo");
            } catch (Exception ignored) {
            }
        }
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> gitHubClient.getLastUpdated("user", "repo")).isInstanceOf(Exception.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(1000);
    }
}
