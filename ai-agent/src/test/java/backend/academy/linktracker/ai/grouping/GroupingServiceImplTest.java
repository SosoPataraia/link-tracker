package backend.academy.linktracker.ai.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.avro.Priority;
import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupingServiceImplTest {

    private static final long WINDOW_MS = 200L;

    private GroupingServiceImpl service;

    @BeforeEach
    void setUp() {
        var filtering = new AiAgentProperties.Filtering(List.of(), List.of(), 5);
        var summarization = new AiAgentProperties.Summarization(500, "stub");
        var aiApi = new AiAgentProperties.AiApi("http://localhost", "");
        var prioritization = new AiAgentProperties.Prioritization(List.of("critical"), List.of("typo"));
        var grouping = new AiAgentProperties.Grouping(WINDOW_MS);
        var properties = new AiAgentProperties(filtering, summarization, aiApi, prioritization, grouping);
        service = new GroupingServiceImpl(properties);
    }

    @Test
    void whenMultipleUpdatesForSameChatId_thenGroupedIntoOne() {
        List<ProcessedUpdateEvent> emitted = new CopyOnWriteArrayList<>();

        var event1 = event(1L, "First update", List.of(111L), Priority.MEDIUM);
        var event2 = event(2L, "Second update", List.of(111L), Priority.LOW);
        var event3 = event(3L, "Third update", List.of(111L), Priority.HIGH);

        service.accept(event1, emitted::add);
        service.accept(event2, emitted::add);
        service.accept(event3, emitted::add);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(emitted).hasSize(1);
            ProcessedUpdateEvent merged = emitted.getFirst();
            assertThat(merged.getDescription().toString()).contains("1. First update");
            assertThat(merged.getDescription().toString()).contains("2. Second update");
            assertThat(merged.getDescription().toString()).contains("3. Third update");
            assertThat(merged.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(merged.getTgChatIds()).containsExactly(111L);
        });
    }

    @Test
    void whenSingleUpdateForChatId_thenPassesThroughUnchanged() {
        List<ProcessedUpdateEvent> emitted = new CopyOnWriteArrayList<>();

        var event = event(42L, "Single update description", List.of(222L), Priority.MEDIUM);
        service.accept(event, emitted::add);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(emitted).hasSize(1);
            assertThat(emitted.getFirst().getDescription().toString()).isEqualTo("Single update description");
            assertThat(emitted.getFirst().getPriority()).isEqualTo(Priority.NORMAL);
        });
    }

    @Test
    void whenEventHasMultipleChatIds_eachChatIdBufferedSeparately() {
        List<ProcessedUpdateEvent> emitted = new CopyOnWriteArrayList<>();

        var event = event(10L, "Shared update", List.of(100L, 200L), Priority.HIGH);
        service.accept(event, emitted::add);

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(emitted).hasSize(2));
    }

    @Test
    void whenMixedPriorities_thenMaxIsUsed() {
        List<ProcessedUpdateEvent> emitted = new CopyOnWriteArrayList<>();

        service.accept(event(1L, "low priority update", List.of(333L), Priority.LOW), emitted::add);
        service.accept(event(2L, "high priority update", List.of(333L), Priority.HIGH), emitted::add);

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            assertThat(emitted).hasSize(1);
            assertThat(emitted.getFirst().getPriority()).isEqualTo(Priority.HIGH);
        });
    }

    private ProcessedUpdateEvent event(long id, String description, List<Long> chatIds, Priority priority) {
        return ProcessedUpdateEvent.newBuilder()
                .setId(id)
                .setDescription(description)
                .setTgChatIds(chatIds)
                .setPriority(priority)
                .build();
    }
}
