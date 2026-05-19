package backend.academy.linktracker.ai.processor;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.ai.config.KafkaTopicProperties;
import backend.academy.linktracker.ai.filter.FilterResult;
import backend.academy.linktracker.ai.filter.UpdateFilter;
import backend.academy.linktracker.ai.summarizer.Summarizer;
import backend.academy.linktracker.avro.Priority;
import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import backend.academy.linktracker.avro.RawUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateProcessorImpl implements UpdateProcessor {

    private final UpdateFilter updateFilter;
    private final Summarizer summarizer;
    private final KafkaTemplate<String, ProcessedUpdateEvent> kafkaTemplate;
    private final AiAgentProperties properties;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void process(RawUpdateEvent event) {
        FilterResult result = updateFilter.apply(event);
        if (!result.passed()) {
            log.atInfo()
                .addKeyValue("id", event.getId())
                .addKeyValue("reason", result.reason())
                .log("processor.filtered");
            return;
        }

        String description = event.getDescription() != null
            ? event.getDescription().toString()
            : "";

        if (description.length() > properties.summarization().threshold()) {
            description = summarizer.summarize(description);
        }

        var processed = ProcessedUpdateEvent.newBuilder()
            .setId(event.getId())
            .setDescription(description)
            .setTgChatIds(event.getTgChatIds())
            .setPriority(Priority.NORMAL)
            .build();

        kafkaTemplate.send(kafkaTopicProperties.processedUpdates(), String.valueOf(event.getId()), processed);
        log.atInfo()
            .addKeyValue("id", event.getId())
            .addKeyValue("topic", kafkaTopicProperties.processedUpdates())
            .log("processor.published");
    }
}
