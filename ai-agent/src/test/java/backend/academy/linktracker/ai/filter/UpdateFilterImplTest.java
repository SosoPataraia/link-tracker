package backend.academy.linktracker.ai.filter;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.avro.RawUpdateEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFilterImplTest {

    private UpdateFilterImpl filter;

    @BeforeEach
    void setUp() {
        var filtering = new AiAgentProperties.Filtering(List.of("spam", "ads", "promo"), List.of("bot-user"), 20);
        var summarization = new AiAgentProperties.Summarization(500, "stub");
        var aiApi = new AiAgentProperties.AiApi("http://localhost", "");
        var prioritization = new AiAgentProperties.Prioritization(List.of("critical"), List.of("typo"));
        var grouping = new AiAgentProperties.Grouping(30000L);
        var properties = new AiAgentProperties(filtering, summarization, aiApi, prioritization, grouping);
        filter = new UpdateFilterImpl(properties);
    }

    @Test
    void whenDescriptionContainsStopWord_thenFiltered() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
                .setId(1L)
                .setDescription("This is a spam message with enough length to pass min-length")
                .setAuthor("normal-user")
                .setTgChatIds(List.of())
                .build();

        FilterResult result = filter.apply(event);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("stop-word");
    }

    @Test
    void whenAuthorIsExcluded_thenFiltered() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
                .setId(2L)
                .setDescription("This is a perfectly normal message with sufficient length here")
                .setAuthor("bot-user")
                .setTgChatIds(List.of())
                .build();

        FilterResult result = filter.apply(event);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("excluded-author");
    }

    @Test
    void whenDescriptionTooShort_thenFiltered() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
                .setId(3L)
                .setDescription("Too short")
                .setAuthor("normal-user")
                .setTgChatIds(List.of())
                .build();

        FilterResult result = filter.apply(event);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).contains("min-length");
    }

    @Test
    void whenUpdateIsValid_thenPasses() {
        RawUpdateEvent event = RawUpdateEvent.newBuilder()
                .setId(4L)
                .setDescription("This is a perfectly normal message with sufficient length here")
                .setAuthor("normal-user")
                .setTgChatIds(List.of(111L, 222L))
                .build();

        FilterResult result = filter.apply(event);

        assertThat(result.passed()).isTrue();
        assertThat(result.reason()).isNull();
    }
}
