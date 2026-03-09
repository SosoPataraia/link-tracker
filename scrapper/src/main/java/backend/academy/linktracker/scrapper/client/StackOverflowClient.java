package backend.academy.linktracker.scrapper.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StackOverflowClient {

    private final RestClient stackOverflowRestClient;

    /**
     * Returns last activity time for a StackOverflow question.
     * URL format: https://stackoverflow.com/questions/{questionId}/...
     */
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class QuestionResponse {
        private List<QuestionItem> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class QuestionItem {

        @JsonProperty("question_id")
        private Long questionId;

        @JsonProperty("last_activity_date")
        private Long lastActivityDate;

        private String title;
    }
}
