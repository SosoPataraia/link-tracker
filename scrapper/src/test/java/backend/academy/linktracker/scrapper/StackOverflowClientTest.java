package backend.academy.linktracker.scrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.client.StackOverflowClientImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@EnableWireMock
class StackOverflowClientTest {

    @InjectWireMock
    WireMockServer wireMock;

    @Test
    void getLastActivity_parsesCorrectly() {
        stubFor(get(urlPathEqualTo("/questions/123456"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "items": [
                                    {
                                      "question_id": 123456,
                                      "last_activity_date": 1705312200,
                                      "title": "Test question"
                                    }
                                  ]
                                }
                                """)));

        var client = new StackOverflowClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        Instant result = client.getLastActivity(123456L);
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Instant.ofEpochSecond(1705312200L));
    }

    @Test
    void getLastActivity_handlesErrorResponse() {
        stubFor(get(urlPathEqualTo("/questions/999")).willReturn(aResponse().withStatus(503)));

        var client = new StackOverflowClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        Instant result = client.getLastActivity(999L);
        assertThat(result).isNull();
    }

    @Test
    void getLastActivity_handlesEmptyItems() {
        stubFor(get(urlPathEqualTo("/questions/111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"items\": []}")));

        var client = new StackOverflowClientImpl(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());

        Instant result = client.getLastActivity(111L);
        assertThat(result).isNull();
    }
}
