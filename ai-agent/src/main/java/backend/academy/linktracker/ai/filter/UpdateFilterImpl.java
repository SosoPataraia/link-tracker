package backend.academy.linktracker.ai.filter;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.avro.RawUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateFilterImpl implements UpdateFilter {

    private final AiAgentProperties properties;

    @Override
    public FilterResult apply(RawUpdateEvent event) {
        String description = event.getDescription() != null ? event.getDescription().toString() : "";
        String author = event.getAuthor() != null ? event.getAuthor().toString() : "";

        FilterResult stopWordResult = checkStopWords(description);
        if (!stopWordResult.passed()) {
            return stopWordResult;
        }

        FilterResult authorResult = checkExcludedAuthor(author);
        if (!authorResult.passed()) {
            return authorResult;
        }

        return checkMinLength(description);
    }

    private FilterResult checkStopWords(String description) {
        String lower = description.toLowerCase();
        return properties.filtering().stopWords().stream()
            .filter(word -> lower.contains(word.toLowerCase()))
            .findFirst()
            .map(word -> FilterResult.reject("stop-word: " + word))
            .orElse(FilterResult.pass());
    }

    private FilterResult checkExcludedAuthor(String author) {
        boolean isExcluded = properties.filtering().excludedAuthors().stream()
            .anyMatch(excludedAuthor -> excludedAuthor.equalsIgnoreCase(author));
        return isExcluded
            ? FilterResult.reject("excluded-author: " + author)
            : FilterResult.pass();
    }

    private FilterResult checkMinLength(String description) {
        int minLength = properties.filtering().minLength();
        return description.length() < minLength
            ? FilterResult.reject("min-length: " + description.length() + " < " + minLength)
            : FilterResult.pass();
    }
}
