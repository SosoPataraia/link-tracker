package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.stackoverflow.AnswerItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.CommentItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionResponse;
import backend.academy.linktracker.scrapper.dto.stackoverflow.StackOverflowItemsResponse;
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
            log.atError().addKeyValue("questionId", questionId).log("stackoverflow.activity.fetch.failed", e);
            return null;
        }
    }

    @Override
    public Optional<QuestionItem> getQuestion(long questionId) {
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
    }

    @Override
    public List<AnswerItem> getNewAnswers(long questionId, Instant since) {
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
        if (response == null || response.getItems() == null) {
            return List.of();
        }
        return response.getItems();
    }

    @Override
    public List<CommentItem> getNewComments(long questionId, Instant since) {
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
        if (response == null || response.getItems() == null) {
            return List.of();
        }
        return response.getItems();
    }
}
