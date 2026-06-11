package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
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
    StackOverflowClient stackOverflowClient;

    @Autowired
    BotClient botClient;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void wireMockProperties(DynamicPropertyRegistry registry) {
        // will be overridden per-test via WireMock injection, but base URL needed for RestClient bean
    }

    @BeforeEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.circuitBreaker("githubClient").reset();
        circuitBreakerRegistry.circuitBreaker("stackOverflowClient").reset();
        circuitBreakerRegistry.circuitBreaker("botClient").reset();
    }

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
    void getLastUpdated_constantBackoff_respectsWaitDuration() {
        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> gitHubClient.getLastUpdated("user", "repo")).isInstanceOf(Exception.class);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(200);
        verify(3, getRequestedFor(urlPathEqualTo("/repos/user/repo")));
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold_github() {
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

    @Test
    void circuitBreaker_halfOpen_transitionsToClosed_github() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("githubClient");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        stubFor(get(urlPathEqualTo("/repos/user/repo"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"full_name":"user/repo","pushed_at":"2024-01-15T10:30:00Z","updated_at":"2024-01-15T10:30:00Z"}
                                """)));

        for (int i = 0; i < 5; i++) {
            gitHubClient.getLastUpdated("user", "repo");
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreaker_halfOpen_transitionsToOpen_github() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("githubClient");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        stubFor(get(urlPathEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 5; i++) {
            try {
                gitHubClient.getLastUpdated("user", "repo");
            } catch (Exception ignored) {
            }
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold_stackoverflow() {
        stubFor(get(urlPathEqualTo("/questions/123")).willReturn(aResponse().withStatus(500)));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("stackOverflowClient");

        for (int i = 0; i < 5; i++) {
            try {
                stackOverflowClient.getLastActivity(123L);
            } catch (Exception ignored) {
            }
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_halfOpen_transitionsToClosed_stackoverflow() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("stackOverflowClient");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        stubFor(get(urlPathEqualTo("/questions/123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"items":[{"question_id":123,"last_activity_date":1705312200,"title":"Test"}]}
                                """)));

        for (int i = 0; i < 5; i++) {
            stackOverflowClient.getLastActivity(123L);
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreaker_halfOpen_transitionsToOpen_stackoverflow() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("stackOverflowClient");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        stubFor(get(urlPathEqualTo("/questions/123")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 5; i++) {
            try {
                stackOverflowClient.getLastActivity(123L);
            } catch (Exception ignored) {
            }
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_opensAfterFailureThreshold_botClient() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("botClient");

        for (int i = 0; i < 5; i++) {
            cb.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("bot down"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreaker_halfOpen_transitionsToClosed_botClient() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("botClient");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        for (int i = 0; i < 5; i++) {
            cb.onSuccess(0, java.util.concurrent.TimeUnit.NANOSECONDS);
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void circuitBreaker_halfOpen_transitionsToOpen_botClient() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("botClient");

        cb.transitionToOpenState();
        cb.transitionToHalfOpenState();

        for (int i = 0; i < 5; i++) {
            cb.onError(0, java.util.concurrent.TimeUnit.NANOSECONDS, new RuntimeException("bot down"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
