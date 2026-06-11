package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.stackoverflow.AnswerItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.CommentItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionResponse;
import backend.academy.linktracker.scrapper.dto.stackoverflow.StackOverflowItemsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StackOverflowClientImpl implements StackOverflowClient {

    private static final String STACKOVERFLOW_CLIENT_NAME = "stackOverflowClient";

    private final RestClient stackOverflowRestClient;

    @Override
    @Retry(name = STACKOVERFLOW_CLIENT_NAME)
    @CircuitBreaker(name = STACKOVERFLOW_CLIENT_NAME)
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
            log.atError()
                    .addKeyValue("questionId", questionId)
                    .addKeyValue("error", e.getMessage())
                    .log("stackoverflow.getLastActivity.failed");
            throw e;
        }
    }

    @Override
    @Retry(name = STACKOVERFLOW_CLIENT_NAME)
    @CircuitBreaker(name = STACKOVERFLOW_CLIENT_NAME)
    public Optional<QuestionItem> getQuestion(long questionId) {
        try {
            var response = stackOverflowRestClient
                    .get()
                    .uri("/questions/{id}?site=stackoverflow&filter=withbody", questionId)
                    .retrieve()
                    .body(QuestionResponse.class);
            if (response != null
                    && response.getItems() != null
                    && !response.getItems().isEmpty()) {
                return Optional.of(response.getItems().getFirst());
            }
            return Optional.empty();
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("questionId", questionId)
                    .addKeyValue("error", e.getMessage())
                    .log("stackoverflow.getQuestion.failed");
            throw e;
        }
    }

    @Override
    @Retry(name = STACKOVERFLOW_CLIENT_NAME)
    @CircuitBreaker(name = STACKOVERFLOW_CLIENT_NAME)
    public List<AnswerItem> getNewAnswers(long questionId, Instant since) {
        try {
            long sinceEpoch = since.getEpochSecond();
            var response = stackOverflowRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/answers")
                            .queryParam("site", "stackoverflow")
                            .queryParam("filter", "withbody")
                            .queryParam("fromdate", sinceEpoch)
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("pagesize", "50")
                            .build(questionId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<StackOverflowItemsResponse<AnswerItem>>() {});
            if (response == null || response.getItems() == null) return List.of();
            return response.getItems();
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("questionId", questionId)
                    .addKeyValue("error", e.getMessage())
                    .log("stackoverflow.getNewAnswers.failed");
            throw e;
        }
    }

    @Override
    @Retry(name = STACKOVERFLOW_CLIENT_NAME)
    @CircuitBreaker(name = STACKOVERFLOW_CLIENT_NAME)
    public List<CommentItem> getNewComments(long questionId, Instant since) {
        try {
            long sinceEpoch = since.getEpochSecond();
            var response = stackOverflowRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/questions/{id}/comments")
                            .queryParam("site", "stackoverflow")
                            .queryParam("filter", "withbody")
                            .queryParam("fromdate", sinceEpoch)
                            .queryParam("order", "desc")
                            .queryParam("sort", "creation")
                            .queryParam("pagesize", "50")
                            .build(questionId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<StackOverflowItemsResponse<CommentItem>>() {});
            if (response == null || response.getItems() == null) return List.of();
            return response.getItems();
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("questionId", questionId)
                    .addKeyValue("error", e.getMessage())
                    .log("stackoverflow.getNewComments.failed");
            throw e;
        }
    }
}
