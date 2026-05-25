package backend.academy.linktracker.ai.processor;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UpdateProcessorImpl implements UpdateProcessor {

    private final UpdateFilter updateFilter;
    private final Summarizer summarizer;
    private final PrioritizationService prioritizationService;
    private final GroupingService groupingService;
    private final KafkaTemplate<String, ProcessedUpdateEvent> kafkaTemplate;
    private final AiAgentProperties properties;
    private final KafkaTopicProperties kafkaTopicProperties;

    public UpdateProcessorImpl(
            UpdateFilter updateFilter,
            Summarizer summarizer,
            PrioritizationService prioritizationService,
            GroupingService groupingService,
            @Qualifier("processedKafkaTemplate") KafkaTemplate<String, ProcessedUpdateEvent> kafkaTemplate,
            AiAgentProperties properties,
            KafkaTopicProperties kafkaTopicProperties) {
        this.updateFilter = updateFilter;
        this.summarizer = summarizer;
        this.prioritizationService = prioritizationService;
        this.groupingService = groupingService;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.kafkaTopicProperties = kafkaTopicProperties;
    }

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

        String description =
                event.getDescription() != null ? event.getDescription().toString() : "";

        if (description.length() > properties.summarization().threshold()) {
            description = summarizer.summarize(description);
        }

        Priority priority = prioritizationService.determine(description);

        var processed = ProcessedUpdateEvent.newBuilder()
                .setId(event.getId())
                .setDescription(description)
                .setTgChatIds(event.getTgChatIds())
                .setPriority(priority.toAvro())
                .build();

        log.atInfo()
                .addKeyValue("id", event.getId())
                .addKeyValue("priority", priority)
                .log("processor.prioritized");

        groupingService.accept(processed, this::publish);
    }

    private void publish(ProcessedUpdateEvent event) {
        kafkaTemplate.send(kafkaTopicProperties.processedUpdates(), String.valueOf(event.getId()), event);
        log.atInfo()
                .addKeyValue("id", event.getId())
                .addKeyValue("topic", kafkaTopicProperties.processedUpdates())
                .addKeyValue("priority", event.getPriority())
                .log("processor.published");
    }
}
