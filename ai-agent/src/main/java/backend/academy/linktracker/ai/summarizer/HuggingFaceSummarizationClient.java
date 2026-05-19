package backend.academy.linktracker.ai.summarizer;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai-agent.summarization.mode", havingValue = "ai", matchIfMissing = true)
public class HuggingFaceSummarizationClient implements SummarizationClient {

    private final RestClient restClient;
    private final AiAgentProperties properties;

    @Override
    public String summarize(String text) {
        String token = properties.aiApi().token();
        if (token == null || token.isBlank()) {
            log.atWarn().addKeyValue("reason", "no-token").log("summarizer.ai.skipped");
            return text;
        }

        try {
            var request = Map.of("inputs", text);
            var response = restClient
                    .post()
                    .uri(properties.aiApi().url())
                    .header("Authorization", "Bearer " + token)
                    .body(request)
                    .retrieve()
                    .body(List.class);

            if (response != null && !response.isEmpty()) {
                var first = response.getFirst();
                if (first instanceof Map<?, ?> map && map.get("summary_text") instanceof String summary) {
                    return summary;
                }
            }
        } catch (Exception e) {
            log.atWarn().addKeyValue("reason", e.getMessage()).log("summarizer.ai.failed.fallback-to-original");
        }
        return text;
    }
}
