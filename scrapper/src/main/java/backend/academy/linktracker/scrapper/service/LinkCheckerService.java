package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.AnswerItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.CommentItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionItem;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkCheckerService {

    private static final Pattern GITHUB_PATTERN = Pattern.compile("https?://github\\.com/([^/]+)/([^/?#]+).*");
    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("https?://stackoverflow\\.com/questions/(\\d+).*");

    private final LinkRepository linkRepository;
    private final GitHubClient gitHubClient;
    private final StackOverflowClient stackOverflowClient;
    private final BotClient botClient;

    /**
     * Checks all links and sends updates. Called by the scheduler.
     * Groups links by URL so each external API is called once per unique URL.
     */
    public void checkLinks(Collection<TrackedLink> links) {
        Map<String, List<TrackedLink>> byUrl = links.stream().collect(Collectors.groupingBy(TrackedLink::getUrl));

        byUrl.forEach((url, subscribers) -> {
            try {
                checkUrl(url, subscribers);
            } catch (Exception e) {
                log.error("Unhandled error checking url={}", url, e);
            }
        });
    }

    private void checkUrl(String url, List<TrackedLink> subscribers) {
        Instant since = subscribers.stream()
                .map(TrackedLink::getLastChecked)
                .filter(t -> t != null)
                .min(Instant::compareTo)
                .orElse(Instant.EPOCH);

        List<UpdateDescription> updates = new ArrayList<>();

        Matcher githubMatcher = GITHUB_PATTERN.matcher(url);
        Matcher soMatcher = STACKOVERFLOW_PATTERN.matcher(url);

        if (githubMatcher.matches()) {
            updates.addAll(checkGitHub(githubMatcher.group(1), githubMatcher.group(2), since));
        } else if (soMatcher.matches()) {
            updates.addAll(checkStackOverflow(Long.parseLong(soMatcher.group(1)), since));
        } else {
            log.debug("Unsupported URL format: {}", url);
            return;
        }

        if (!updates.isEmpty()) {
            List<Long> chatIds =
                    subscribers.stream().map(TrackedLink::getChatId).distinct().toList();
            long representativeLinkId = subscribers.getFirst().getId();

            for (UpdateDescription desc : updates) {
                var linkUpdate = new LinkUpdate(representativeLinkId, url, desc.format(url), chatIds);
                botClient.sendUpdate(linkUpdate);
                log.info("Sent update type={} url={} chatIds={}", desc.getType(), url, chatIds);
            }
        }

        Instant now = Instant.now();
        subscribers.stream()
                .map(TrackedLink::getId)
                .distinct()
                .forEach(id -> linkRepository.updateLastChecked(id, now));
    }

    private List<UpdateDescription> checkGitHub(String owner, String repo, Instant since) {
        List<UpdateDescription> result = new ArrayList<>();

        List<IssueItem> newIssues = gitHubClient.getNewIssues(owner, repo, since);
        for (IssueItem issue : newIssues) {
            result.add(new UpdateDescription(
                    UpdateDescription.Type.NEW_ISSUE,
                    null,
                    issue.getTitle(),
                    issue.getUser() != null ? issue.getUser().getLogin() : "unknown",
                    issue.getCreatedAt() != null ? issue.getCreatedAt() : Instant.now(),
                    UpdateDescription.truncate(issue.getBody())));
        }

        List<IssueItem> newPRs = gitHubClient.getNewPullRequests(owner, repo, since);
        for (IssueItem pr : newPRs) {
            result.add(new UpdateDescription(
                    UpdateDescription.Type.NEW_PR,
                    null,
                    pr.getTitle(),
                    pr.getUser() != null ? pr.getUser().getLogin() : "unknown",
                    pr.getCreatedAt() != null ? pr.getCreatedAt() : Instant.now(),
                    UpdateDescription.truncate(pr.getBody())));
        }

        return result;
    }

    private List<UpdateDescription> checkStackOverflow(long questionId, Instant since) {
        List<UpdateDescription> result = new ArrayList<>();

        String questionTitle = stackOverflowClient
                .getQuestion(questionId)
                .map(QuestionItem::getTitle)
                .orElse(null);

        List<AnswerItem> newAnswers = stackOverflowClient.getNewAnswers(questionId, since);
        for (AnswerItem answer : newAnswers) {
            String username = answer.getOwner() != null ? answer.getOwner().getDisplayName() : "unknown";
            Instant createdAt =
                    answer.getCreationDate() != null ? Instant.ofEpochSecond(answer.getCreationDate()) : Instant.now();
            result.add(new UpdateDescription(
                    UpdateDescription.Type.NEW_ANSWER,
                    questionTitle,
                    null,
                    username,
                    createdAt,
                    UpdateDescription.truncate(answer.getBody())));
        }

        List<CommentItem> newComments = stackOverflowClient.getNewComments(questionId, since);
        for (CommentItem comment : newComments) {
            String username = comment.getOwner() != null ? comment.getOwner().getDisplayName() : "unknown";
            Instant createdAt = comment.getCreationDate() != null
                    ? Instant.ofEpochSecond(comment.getCreationDate())
                    : Instant.now();
            result.add(new UpdateDescription(
                    UpdateDescription.Type.NEW_COMMENT,
                    questionTitle,
                    null,
                    username,
                    createdAt,
                    UpdateDescription.truncate(comment.getBody())));
        }

        return result;
    }
}
