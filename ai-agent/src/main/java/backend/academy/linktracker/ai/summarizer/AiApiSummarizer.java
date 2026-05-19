package backend.academy.linktracker.ai.summarizer;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai-agent.summarization.mode", havingValue = "ai", matchIfMissing = true)
public class AiApiSummarizer implements Summarizer {

    private final SummarizationClient summarizationClient;
    private final AiAgentProperties properties;

    @Override
    public String summarize(String text) {
        log.atInfo()
                .addKeyValue("originalLength", text.length())
                .addKeyValue("threshold", properties.summarization().threshold())
                .log("summarizer.ai.invoking");
        return summarizationClient.summarize(text);
    }
}
