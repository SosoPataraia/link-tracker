package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkCheckerService {

    private static final Pattern GITHUB_PATTERN = Pattern.compile("https?://github\\.com/([^/]+)/([^/]+).*");
    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("https?://stackoverflow\\.com/questions/(\\d+).*");

    private final LinkRepository linkRepository;
    private final GitHubClient gitHubClient; // now an interface
    private final StackOverflowClient stackOverflowClient; // now an interface
    private final BotClient botClient; // now an interface

    public void checkAllLinks() {
        var allLinks = linkRepository.findAll();
        log.info("Checking {} links for updates", allLinks.size());

        var linksByUrl = allLinks.stream().collect(java.util.stream.Collectors.groupingBy(TrackedLink::getUrl));

        linksByUrl.forEach((url, links) -> {
            try {
                Instant lastUpdated = fetchLastUpdated(url);
                if (lastUpdated == null) return;

                for (TrackedLink link : links) {
                    if (link.getLastUpdated() != null && lastUpdated.isAfter(link.getLastUpdated())) {
                        notifyUpdate(link, links);
                        break;
                    } else if (link.getLastUpdated() == null) {
                        link.setLastUpdated(lastUpdated);
                    }
                }
                links.forEach(l -> l.setLastUpdated(lastUpdated));
            } catch (Exception e) {
                log.error("Error checking url={}: {}", url, e.getMessage());
            }
        });
    }

    private void notifyUpdate(TrackedLink link, List<TrackedLink> allLinksForUrl) {
        List<Long> chatIds =
                allLinksForUrl.stream().map(TrackedLink::getChatId).distinct().toList();
        var update = new LinkUpdate(link.getId(), link.getUrl(), "Обнаружены изменения по ссылке", chatIds);
        log.info("Sending update for url={} to {} chats", link.getUrl(), chatIds.size());
        botClient.sendUpdate(update);
    }

    private Instant fetchLastUpdated(String url) {
        Matcher githubMatcher = GITHUB_PATTERN.matcher(url);
        if (githubMatcher.matches()) {
            return gitHubClient.getLastUpdated(githubMatcher.group(1), githubMatcher.group(2));
        }
        Matcher soMatcher = STACKOVERFLOW_PATTERN.matcher(url);
        if (soMatcher.matches()) {
            return stackOverflowClient.getLastActivity(Long.parseLong(soMatcher.group(1)));
        }
        log.debug("Unsupported URL format: {}", url);
        return null;
    }
}
