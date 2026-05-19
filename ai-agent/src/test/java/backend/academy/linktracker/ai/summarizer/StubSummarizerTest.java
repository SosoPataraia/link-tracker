package backend.academy.linktracker.ai.summarizer;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubSummarizerTest {

    private StubSummarizer summarizer;
    private static final int THRESHOLD = 50;

    @BeforeEach
    void setUp() {
        var filtering = new AiAgentProperties.Filtering(List.of(), List.of(), 5);
        var summarization = new AiAgentProperties.Summarization(THRESHOLD, "stub");
        var aiApi = new AiAgentProperties.AiApi("http://localhost", "");
        var properties = new AiAgentProperties(filtering, summarization, aiApi);
        summarizer = new StubSummarizer(properties);
    }

    @Test
    void whenTextExceedsThreshold_thenTruncatedWithEllipsis() {
        String longText = "A".repeat(200);

        String result = summarizer.summarize(longText);

        assertThat(result).endsWith("...");
        assertThat(result.length()).isEqualTo(THRESHOLD + 3);
    }

    @Test
    void whenTextAtThresholdBoundary_thenSummarizerStillTruncates() {
        String boundaryText = "B".repeat(THRESHOLD);

        String result = summarizer.summarize(boundaryText);

        assertThat(result).endsWith("...");
    }
}
