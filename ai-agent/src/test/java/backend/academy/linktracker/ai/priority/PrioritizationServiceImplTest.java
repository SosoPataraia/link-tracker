package backend.academy.linktracker.ai.priority;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrioritizationServiceImplTest {

    private PrioritizationServiceImpl service;

    @BeforeEach
    void setUp() {
        var filtering = new AiAgentProperties.Filtering(List.of("spam"), List.of("bot-user"), 20);
        var summarization = new AiAgentProperties.Summarization(500, "stub");
        var aiApi = new AiAgentProperties.AiApi("http://localhost", "");
        var prioritization = new AiAgentProperties.Prioritization(
                List.of("critical", "urgent", "breaking", "security"), List.of("minor", "typo", "chore", "docs"));
        var grouping = new AiAgentProperties.Grouping(30000L);
        var properties = new AiAgentProperties(filtering, summarization, aiApi, prioritization, grouping);
        service = new PrioritizationServiceImpl(properties);
    }

    @Test
    void whenDescriptionContainsHighKeyword_thenPriorityIsHigh() {
        Priority result = service.determine("critical bug fix in authentication module");
        assertThat(result).isEqualTo(Priority.HIGH);
    }

    @Test
    void whenDescriptionContainsHighKeyword_caseInsensitive_thenPriorityIsHigh() {
        Priority result = service.determine("URGENT: server is down");
        assertThat(result).isEqualTo(Priority.HIGH);
    }

    @Test
    void whenDescriptionContainsNoKeywords_thenPriorityIsMedium() {
        Priority result = service.determine("updated the user profile page with new design");
        assertThat(result).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void whenDescriptionIsNull_thenPriorityIsMedium() {
        Priority result = service.determine(null);
        assertThat(result).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void whenDescriptionIsBlank_thenPriorityIsMedium() {
        Priority result = service.determine("   ");
        assertThat(result).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void whenDescriptionContainsLowKeyword_thenPriorityIsLow() {
        Priority result = service.determine("fix typo in readme");
        assertThat(result).isEqualTo(Priority.LOW);
    }

    @Test
    void whenDescriptionContainsLowKeyword_caseInsensitive_thenPriorityIsLow() {
        Priority result = service.determine("DOCS: update contributing guide");
        assertThat(result).isEqualTo(Priority.LOW);
    }

    @Test
    void whenDescriptionContainsBothHighAndLowKeywords_thenPriorityIsHigh() {
        Priority result = service.determine("critical typo in security notice");
        assertThat(result).isEqualTo(Priority.HIGH);
    }
}
