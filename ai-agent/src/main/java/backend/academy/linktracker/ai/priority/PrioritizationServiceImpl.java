package backend.academy.linktracker.ai.priority;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrioritizationServiceImpl implements PrioritizationService {

    private final AiAgentProperties properties;

    @Override
    public Priority determine(String description) {
        if (description == null || description.isBlank()) {
            return Priority.MEDIUM;
        }

        String lower = description.toLowerCase();

        boolean hasHigh =
                properties.prioritization().highKeywords().stream().anyMatch(kw -> lower.contains(kw.toLowerCase()));
        if (hasHigh) {
            return Priority.HIGH;
        }

        boolean hasLow =
                properties.prioritization().lowKeywords().stream().anyMatch(kw -> lower.contains(kw.toLowerCase()));
        if (hasLow) {
            return Priority.LOW;
        }

        return Priority.MEDIUM;
    }
}
