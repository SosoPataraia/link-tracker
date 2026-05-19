package backend.academy.linktracker.ai.summarizer;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai-agent.summarization.mode", havingValue = "stub")
public class StubSummarizer implements Summarizer {

    private final AiAgentProperties properties;

    @Override
    public String summarize(String text) {
        int threshold = properties.summarization().threshold();
        log.atInfo()
            .addKeyValue("originalLength", text.length())
            .addKeyValue("threshold", threshold)
            .log("summarizer.stub.truncating");
        return text.substring(0, threshold) + "...";
    }
}
