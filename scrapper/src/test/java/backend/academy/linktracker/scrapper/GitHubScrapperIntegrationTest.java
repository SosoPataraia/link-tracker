package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClientImpl;
import backend.academy.linktracker.scrapper.client.StackOverflowClientImpl;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.InMemoryLinkRepository;
import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@EnableWireMock
@ExtendWith(MockitoExtension.class)
class GitHubScrapperIntegrationTest {

    @InjectWireMock
    WireMockServer wireMock;

    @Mock
    BotClient botClient;

    InMemoryLinkRepository linkRepository;
    LinkCheckerService service;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepository();
        var gitHubClient = new GitHubClientImpl(
            RestClient.builder().baseUrl(wireMock.baseUrl()).build());
        var soClient = new StackOverflowClientImpl(
            RestClient.builder().baseUrl(wireMock.baseUrl()).build());
        service = new LinkCheckerService(linkRepository, gitHubClient, soClient, botClient);
    }

    @Test
    void newIssue_formatsMessageWithAllRequiredFields() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                                [
                                  {
                                    "number": 42,
                                    "title": "NPE in login flow",
                                    "body": "Stack trace: java.lang.NullPointerException at LoginService.java:42",
                                    "created_at": "2024-01-15T10:00:00Z",
                                    "user": { "login": "alice" }
                                  }
                                ]
                                """)));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("[]")));

        linkRepository.save(link(100L, "https://github.com/user/repo"));

        service.checkLinks(linkRepository.findAll());

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("New Issue");
        assertThat(desc).contains("NPE in login flow");       // item title
        assertThat(desc).contains("alice");                    // username
        assertThat(desc).contains("2024-01-15");               // created_at
        assertThat(desc).contains("Stack trace");              // preview
        assertThat(captor.getValue().getUrl()).isEqualTo("https://github.com/user/repo");
        assertThat(captor.getValue().getTgChatIds()).containsExactly(100L);
    }

    @Test
    void newPR_formatsMessageWithAllRequiredFields() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("[]")));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                                [
                                  {
                                    "number": 7,
                                    "title": "Add dark mode",
                                    "body": "This PR adds dark mode support to the UI.",
                                    "created_at": "2024-01-16T08:00:00Z",
                                    "user": { "login": "bob" }
                                  }
                                ]
                                """)));

        linkRepository.save(link(200L, "https://github.com/user/repo"));

        service.checkLinks(linkRepository.findAll());

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("New Pull Request");
        assertThat(desc).contains("Add dark mode");
        assertThat(desc).contains("bob");
        assertThat(desc).contains("2024-01-16");
        assertThat(desc).contains("dark mode support");
    }

    @Test
    void apiUnavailable_doesNotSendUpdate_andDoesNotThrow() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
            .willReturn(aResponse().withStatus(503)));
        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
            .willReturn(aResponse().withStatus(503)));

        linkRepository.save(link(100L, "https://github.com/user/repo"));

        // Must not throw
        service.checkLinks(linkRepository.findAll());

        verify(botClient, never()).sendUpdate(any());
    }

    @Test
    void preview_truncatedTo200Chars() {
        String longBody = "A".repeat(300);

        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                                [
                                  {
                                    "number": 1,
                                    "title": "Long body issue",
                                    "body": "%s",
                                    "created_at": "2024-01-15T10:00:00Z",
                                    "user": { "login": "carol" }
                                  }
                                ]
                                """.formatted(longBody))));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("[]")));

        linkRepository.save(link(100L, "https://github.com/user/repo"));

        service.checkLinks(linkRepository.findAll());

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        // Preview in description should be truncated — 200 chars + "..."
        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("A".repeat(200));
        assertThat(desc).contains("...");
        // Should NOT contain the full 300 chars
        assertThat(desc).doesNotContain("A".repeat(201));
    }

    private TrackedLink link(long chatId, String url) {
        var l = new TrackedLink();
        l.setChatId(chatId);
        l.setUrl(url);
        l.setTags(List.of());
        l.setLastChecked(Instant.EPOCH);
        l.setLastUpdated(Instant.now());
        return l;
    }
}
