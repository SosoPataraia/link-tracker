package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClientImpl;
import backend.academy.linktracker.scrapper.client.StackOverflowClientImpl;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.InMemoryChatRepository;
import backend.academy.linktracker.scrapper.service.LinkService;
import backend.academy.linktracker.scrapper.service.LinkServiceImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import java.util.concurrent.Executors;
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
    LinkService service;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepository();
        var gitHubClient = new GitHubClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());
        var soClient = new StackOverflowClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());
        var props = new SchedulerProperties();
        service = new LinkServiceImpl(
                linkRepository,
                new InMemoryChatRepository(),
                gitHubClient,
                soClient,
                botClient,
                props,
                Executors.newSingleThreadExecutor());
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

        linkRepository.save(link(100L, "https://github.com/user/repo"));
        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("New Issue");
        assertThat(desc).contains("NPE in login flow");
        assertThat(desc).contains("alice");
        assertThat(desc).contains("2024-01-15");
        assertThat(desc).contains("Stack trace");
        assertThat(captor.getValue().getTgChatIds()).containsExactly(100L);
    }

    @Test
    void newPR_formatsMessageWithAllRequiredFields() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
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
                                    "pull_request": {},
                                    "user": { "login": "bob" }
                                  }
                                ]
                                """)));

        linkRepository.save(link(200L, "https://github.com/user/repo"));
        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("New Pull Request");
        assertThat(desc).contains("Add dark mode");
        assertThat(desc).contains("bob");
    }

    @Test
    void apiUnavailable_sendsFailureNoticeAndDoesNotThrow() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse().withStatus(503)));

        linkRepository.save(link(100L, "https://github.com/user/repo"));
        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getTgChatIds()).containsExactly(100L);
        assertThat(captor.getValue().getDescription()).contains("Не удалось проверить ссылку");
    }

    @Test
    void preview_truncatedTo200CharsIncludingEllipsis() {
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
                                """.formatted("A".repeat(300)))));

        linkRepository.save(link(100L, "https://github.com/user/repo"));
        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("A".repeat(197));
        assertThat(desc).contains("...");
        assertThat(desc).doesNotContain("A".repeat(198));
    }

    private TrackedLink link(long chatId, String url) {
        var l = new TrackedLink();
        l.setChatId(chatId);
        l.setUrl(url);
        l.setTags(java.util.List.of());
        l.setLastChecked(Instant.EPOCH);
        l.setLastUpdated(Instant.now());
        return l;
    }
}
