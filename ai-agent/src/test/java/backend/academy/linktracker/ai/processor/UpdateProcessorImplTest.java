package backend.academy.linktracker.ai.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.ai.config.KafkaTopicProperties;
import backend.academy.linktracker.ai.filter.FilterResult;
import backend.academy.linktracker.ai.filter.UpdateFilter;
import backend.academy.linktracker.ai.grouping.GroupingService;
import backend.academy.linktracker.ai.priority.PrioritizationService;
import backend.academy.linktracker.ai.priority.Priority;
import backend.academy.linktracker.ai.summarizer.Summarizer;
import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import backend.academy.linktracker.avro.RawUpdateEvent;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UpdateProcessorImplTest {

    @Mock
    private UpdateFilter updateFilter;

    @Mock
    private Summarizer summarizer;

    @Mock
    private KafkaTemplate<String, ProcessedUpdateEvent> kafkaTemplate;

    @Mock
    private PrioritizationService prioritizationService;

    @Mock
    private GroupingService groupingService;

    private UpdateProcessorImpl processor;
    private static final int THRESHOLD = 50;
    private static final String PROCESSED_TOPIC = "link.processed-updates";

    @BeforeEach
    void setUp() {
        var properties = getAiAgentProperties();
        var topicProperties = new KafkaTopicProperties("link.raw-updates", PROCESSED_TOPIC);
        doAnswer(invocation -> {
                    var event2 = (ProcessedUpdateEvent) invocation.getArgument(0);
                    Consumer<ProcessedUpdateEvent> callback = invocation.getArgument(1);
                    callback.accept(event2);
                    return null;
                })
                .when(groupingService)
                .accept(any(), any());

        processor = new UpdateProcessorImpl(
                updateFilter,
                summarizer,
                prioritizationService,
                groupingService,
                kafkaTemplate,
                properties,
                topicProperties);
    }

    private static AiAgentProperties getAiAgentProperties() {
        var filtering = new AiAgentProperties.Filtering(List.of(), List.of(), 5);
        var summarization = new AiAgentProperties.Summarization(THRESHOLD, "stub");
        var aiApi = new AiAgentProperties.AiApi("http://localhost", "");
        var prioritization = new AiAgentProperties.Prioritization(List.of("critical"), List.of("typo"));
        var grouping = new AiAgentProperties.Grouping(30000L);
        var properties = new AiAgentProperties(filtering, summarization, aiApi, prioritization, grouping);
        return properties;
    }

    @Test
    void whenFilterRejects_thenNotPublished() {
        RawUpdateEvent event = buildEvent("spam content here", "bot-user");
        when(updateFilter.apply(event)).thenReturn(FilterResult.reject("stop-word: spam"));

        processor.process(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void whenTextExceedsThreshold_thenSummarizerCalled() {
        String longText = "A".repeat(THRESHOLD + 1);
        RawUpdateEvent event = buildEvent(longText, "normal-user");
        when(updateFilter.apply(event)).thenReturn(FilterResult.pass());
        when(summarizer.summarize(longText)).thenReturn("summarized");
        when(prioritizationService.determine(anyString())).thenReturn(Priority.MEDIUM);

        processor.process(event);

        verify(summarizer).summarize(longText);
        verify(kafkaTemplate).send(eq(PROCESSED_TOPIC), anyString(), any(ProcessedUpdateEvent.class));
    }

    @Test
    void whenTextBelowThreshold_thenSummarizerNotCalled() {
        String shortText = "A".repeat(THRESHOLD - 1);
        RawUpdateEvent event = buildEvent(shortText, "normal-user");
        when(updateFilter.apply(event)).thenReturn(FilterResult.pass());
        when(prioritizationService.determine(anyString())).thenReturn(Priority.MEDIUM);

        processor.process(event);

        verify(summarizer, never()).summarize(anyString());
        verify(kafkaTemplate).send(eq(PROCESSED_TOPIC), anyString(), any(ProcessedUpdateEvent.class));
    }

    private RawUpdateEvent buildEvent(String description, String author) {
        return RawUpdateEvent.newBuilder()
                .setId(1L)
                .setDescription(description)
                .setAuthor(author)
                .setTgChatIds(List.of(111L))
                .build();
    }
}
