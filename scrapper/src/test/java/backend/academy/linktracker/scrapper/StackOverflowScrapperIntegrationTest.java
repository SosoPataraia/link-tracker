package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.scrapper.client.GitHubClientImpl;
import backend.academy.linktracker.scrapper.client.StackOverflowClientImpl;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.InMemoryLinkRepository;
import backend.academy.linktracker.scrapper.sender.NotificationSender;
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
class StackOverflowScrapperIntegrationTest {

    @InjectWireMock
    WireMockServer wireMock;

    @Mock
    NotificationSender notificationSender;

    InMemoryLinkRepository linkRepository;
    LinkCheckerService service;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepository();
        var gitHubClient = new GitHubClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());
        var soClient = new StackOverflowClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());
        service = new LinkCheckerService(linkRepository, gitHubClient, soClient, notificationSender);
    }

    @Test
    void newAnswer_formatsMessageWithAllRequiredFields() {
        stubFor(get(urlPathEqualTo("/questions/12345"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "items": [
                                    {
                                      "question_id": 12345,
                                      "title": "How to test Spring Boot apps?",
                                      "last_activity_date": 1705312200,
                                      "owner": { "display_name": "questioner" }
                                    }
                                  ]
                                }
                                """)));

        stubFor(get(urlPathEqualTo("/questions/12345/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "items": [
                                    {
                                      "answer_id": 99,
                                      "creation_date": 1705312200,
                                      "body": "Use @SpringBootTest annotation with Testcontainers.",
                                      "owner": { "display_name": "alice" }
                                    }
                                  ]
                                }
                                """)));

        stubFor(get(urlPathEqualTo("/questions/12345/comments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"items\": []}")));

        linkRepository.save(link(100L, "https://stackoverflow.com/questions/12345/how-to-test"));

        service.checkLinks(linkRepository.findAll());

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(notificationSender).send(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("New Answer");
        assertThat(desc).contains("How to test Spring Boot apps?");
        assertThat(desc).contains("alice");
        assertThat(desc).contains("Use @SpringBootTest");
    }

    @Test
    void newComment_formatsMessageCorrectly() {
        stubFor(get(urlPathEqualTo("/questions/12345"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "items": [
                                    {
                                      "question_id": 12345,
                                      "title": "How to test Spring Boot apps?",
                                      "last_activity_date": 1705312200,
                                      "owner": { "display_name": "questioner" }
                                    }
                                  ]
                                }
                                """)));

        stubFor(get(urlPathEqualTo("/questions/12345/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"items\": []}")));

        stubFor(get(urlPathEqualTo("/questions/12345/comments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "items": [
                                    {
                                      "comment_id": 55,
                                      "creation_date": 1705312200,
                                      "body": "Have you tried MockMvc?",
                                      "owner": { "display_name": "bob" }
                                    }
                                  ]
                                }
                                """)));

        linkRepository.save(link(200L, "https://stackoverflow.com/questions/12345/how-to-test"));

        service.checkLinks(linkRepository.findAll());

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(notificationSender).send(captor.capture());

        String desc = captor.getValue().getDescription();
        assertThat(desc).contains("New Comment");
        assertThat(desc).contains("bob");
        assertThat(desc).contains("Have you tried MockMvc?");
    }

    @Test
    void soApiUnavailable_doesNotSendUpdate_andDoesNotThrow() {
        stubFor(get(urlPathEqualTo("/questions/12345")).willReturn(aResponse().withStatus(503)));
        stubFor(get(urlPathEqualTo("/questions/12345/answers"))
                .willReturn(aResponse().withStatus(503)));
        stubFor(get(urlPathEqualTo("/questions/12345/comments"))
                .willReturn(aResponse().withStatus(503)));

        linkRepository.save(link(100L, "https://stackoverflow.com/questions/12345/test"));

        service.checkLinks(linkRepository.findAll());

        verify(notificationSender, never()).send(any());
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
