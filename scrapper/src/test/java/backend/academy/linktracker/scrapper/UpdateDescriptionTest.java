package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.dto.UpdateDescription;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UpdateDescriptionTest {

    @Test
    void format_newIssue_containsAllFields() {
        var desc = new UpdateDescription(
                UpdateDescription.Type.NEW_ISSUE,
                null,
                "NPE in login",
                "alice",
                Instant.parse("2024-01-15T10:00:00Z"),
                "Stack trace here");

        String result = desc.format("https://github.com/user/repo");

        assertThat(result).contains("New Issue");
        assertThat(result).contains("https://github.com/user/repo");
        assertThat(result).contains("NPE in login");
        assertThat(result).contains("alice");
        assertThat(result).contains("2024-01-15");
        assertThat(result).contains("Stack trace here");
    }

    @Test
    void format_newAnswer_containsQuestionTitle() {
        var desc = new UpdateDescription(
                UpdateDescription.Type.NEW_ANSWER,
                "How to test Spring Boot?",
                null,
                "bob",
                Instant.parse("2024-01-15T10:00:00Z"),
                "Use @SpringBootTest");

        String result = desc.format("https://stackoverflow.com/questions/123/test");

        assertThat(result).contains("New Answer");
        assertThat(result).contains("How to test Spring Boot?");
        assertThat(result).contains("bob");
        assertThat(result).contains("Use @SpringBootTest");
    }

    @Test
    void format_newPR_containsCorrectLabel() {
        var desc = new UpdateDescription(
                UpdateDescription.Type.NEW_PR, null, "Add dark mode", "carol", Instant.now(), "Adds dark mode to UI");

        String result = desc.format("https://github.com/user/repo");

        assertThat(result).contains("New Pull Request");
        assertThat(result).contains("Add dark mode");
        assertThat(result).contains("carol");
    }

    @Test
    void format_newComment_containsCorrectLabel() {
        var desc = new UpdateDescription(
                UpdateDescription.Type.NEW_COMMENT, "Some question", null, "dave", Instant.now(), "Have you tried X?");

        String result = desc.format("https://stackoverflow.com/questions/999/q");

        assertThat(result).contains("New Comment");
        assertThat(result).contains("dave");
        assertThat(result).contains("Have you tried X?");
    }
}
