package backend.academy.linktracker.ai.kafka;

import backend.academy.linktracker.ai.processor.UpdateProcessor;
import backend.academy.linktracker.avro.RawUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawUpdateConsumer {

    private final UpdateProcessor updateProcessor;

    @KafkaListener(
            topics = "${app.kafka.topic.raw-updates:link.raw-updates}",
            groupId = "${spring.kafka.consumer.group-id:ai-agent-group}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(RawUpdateEvent event) {
        log.atInfo()
                .addKeyValue("id", event.getId())
                .addKeyValue("author", event.getAuthor())
                .log("consumer.received");
        updateProcessor.process(event);
    }
}
