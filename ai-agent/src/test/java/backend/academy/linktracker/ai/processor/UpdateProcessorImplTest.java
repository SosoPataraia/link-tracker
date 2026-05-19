package backend.academy.linktracker.ai.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.ai.config.KafkaTopicProperties;
import backend.academy.linktracker.ai.filter.FilterResult;
import backend.academy.linktracker.ai.filter.UpdateFilter;
import backend.academy.linktracker.ai.summarizer.Summarizer;
import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import backend.academy.linktracker.avro.RawUpdateEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class UpdateProcessorImplTest {

    @Mock
    private UpdateFilter updateFilter;

    @Mock
    private Summarizer summarizer;

    @Mock
    private KafkaTemplate<String, ProcessedUpdateEvent> kafkaTemplate;

    private UpdateProcessorImpl processor;
    private static final int THRESHOLD = 50;
    private static final String PROCESSED_TOPIC = "link.processed-updates";

    @BeforeEach
    void setUp() {
        var filtering = new AiAgentProperties.Filtering(List.of(), List.of(), 5);
        var summarization = new AiAgentProperties.Summarization(THRESHOLD, "stub");
        var aiApi = new AiAgentProperties.AiApi("http://localhost", "");
        var properties = new AiAgentProperties(filtering, summarization, aiApi);
        var topicProperties = new KafkaTopicProperties("link.raw-updates", PROCESSED_TOPIC);

        processor = new UpdateProcessorImpl(updateFilter, summarizer, kafkaTemplate, properties, topicProperties);
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

        processor.process(event);

        verify(summarizer).summarize(longText);
        verify(kafkaTemplate).send(eq(PROCESSED_TOPIC), anyString(), any(ProcessedUpdateEvent.class));
    }

    @Test
    void whenTextBelowThreshold_thenSummarizerNotCalled() {
        String shortText = "A".repeat(THRESHOLD - 1);
        RawUpdateEvent event = buildEvent(shortText, "normal-user");
        when(updateFilter.apply(event)).thenReturn(FilterResult.pass());

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
