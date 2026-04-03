package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionResponse;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StackOverflowClientImpl implements StackOverflowClient {

    private final RestClient stackOverflowRestClient;

    @Override
    public Instant getLastActivity(long questionId) {
        try {
            var response = stackOverflowRestClient
                    .get()
                    .uri("/questions/{id}?site=stackoverflow", questionId)
                    .retrieve()
                    .body(QuestionResponse.class);

            if (response != null
                    && response.getItems() != null
                    && !response.getItems().isEmpty()) {
                Long lastActivity = response.getItems().getFirst().getLastActivityDate();
                return lastActivity != null ? Instant.ofEpochSecond(lastActivity) : null;
            }
            return null;
        } catch (RestClientException e) {
            log.error("StackOverflow API error for questionId={}: {}", questionId, e.getMessage());
            return null;
        }
    }
}
