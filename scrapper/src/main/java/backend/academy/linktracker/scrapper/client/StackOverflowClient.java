package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.stackoverflow.AnswerItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.CommentItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StackOverflowClient {

    /**
     * Returns the last_activity_date for the question (used as a fallback / staleness check).
     */
    Instant getLastActivity(long questionId);

    /**
     * Returns the question metadata (title, owner).
     */
    Optional<QuestionItem> getQuestion(long questionId);

    /**
     * Returns answers to the question created after {@code since}.
     */
    List<AnswerItem> getNewAnswers(long questionId, Instant since);

    /**
     * Returns comments on the question created after {@code since}.
     */
    List<CommentItem> getNewComments(long questionId, Instant since);
}
