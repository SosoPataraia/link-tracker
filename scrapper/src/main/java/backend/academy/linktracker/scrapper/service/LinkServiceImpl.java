package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.dto.UpdateDescription;
import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.AnswerItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.CommentItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.QuestionItem;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.util.PreviewUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private static final Pattern GITHUB_PATTERN = Pattern.compile("https?://github\\.com/([^/]+)/([^/?#]+).*");
    private static final Pattern STACKOVERFLOW_PATTERN =
            Pattern.compile("https?://stackoverflow\\.com/questions/(\\d+).*");
    private static final String FAILURE_NOTICE =
            "\u26A0\uFE0F Не удалось проверить ссылку. попробуем снова на следующем цикле.";

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;
    private final GitHubClient gitHubClient;
    private final StackOverflowClient stackOverflowClient;
    private final BotClient botClient;
    private final SchedulerProperties schedulerProperties;
    private final ExecutorService linkCheckExecutor;

    @Override
    @Transactional(readOnly = true)
    public ListLinksResponse getLinks(long chatId) {
        if (!chatRepository.exists(chatId)) {
            return new ListLinksResponse(List.of(), 0);
        }
        List<LinkResponse> responses = linkRepository.findAllByChat(chatId).stream()
                .map(l -> new LinkResponse(l.getId(), l.getUrl(), l.getTags()))
                .toList();
        return new ListLinksResponse(responses, responses.size());
    }

    @Override
    @Transactional
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chat not registered");
        }
        if (linkRepository.findByChatAndUrl(chatId, request.getLink()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Link already tracked");
        }
        var link = new TrackedLink(
                null,
                chatId,
                request.getLink(),
                new ArrayList<>(request.getTags() != null ? request.getTags() : List.of()),
                Instant.now(),
                Instant.now());
        TrackedLink saved = linkRepository.save(link);
        log.atInfo()
                .addKeyValue("url", request.getLink())
                .addKeyValue("chatId", chatId)
                .log("link.added");
        return new LinkResponse(saved.getId(), saved.getUrl(), saved.getTags());
    }

    @Override
    @Transactional
    public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        }
        var existing = linkRepository.findByChatAndUrl(chatId, request.getLink());
        if (existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found");
        }
        linkRepository.remove(chatId, request.getLink());
        log.atInfo()
                .addKeyValue("url", request.getLink())
                .addKeyValue("chatId", chatId)
                .log("link.removed");
        var removed = existing.orElseThrow();
        return new LinkResponse(removed.getId(), removed.getUrl(), removed.getTags());
    }

    @Override
    public void checkAllLinks() {
        Instant tickStart = Instant.now();
        int batchSize = schedulerProperties.getBatchSize();
        Instant cursorLastChecked = Instant.EPOCH;
        long cursorId = 0;
        int totalProcessed = 0;
        int totalFailed = 0;

        log.atInfo()
                .addKeyValue("batchSize", batchSize)
                .addKeyValue("threadCount", schedulerProperties.getThreadCount())
                .log("scheduler.start");

        List<TrackedLink> batch;
        do {
            batch = linkRepository.findLinksToCheck(tickStart, cursorLastChecked, cursorId, batchSize);
            if (batch.isEmpty()) {
                break;
            }

            Map<Long, List<Long>> chatIdsByLink = linkRepository.findChatIdsByLinkIds(
                    batch.stream().map(TrackedLink::getId).toList());

            List<Long> successIds = processBatch(batch, chatIdsByLink);
            if (!successIds.isEmpty()) {
                linkRepository.updateLastChecked(successIds, Instant.now());
            }
            totalProcessed += batch.size();
            totalFailed += batch.size() - successIds.size();

            TrackedLink last = batch.getLast();
            cursorLastChecked = last.getLastChecked() != null ? last.getLastChecked() : Instant.EPOCH;
            cursorId = last.getId();
        } while (batch.size() == batchSize);

        log.atInfo()
                .addKeyValue("totalProcessed", totalProcessed)
                .addKeyValue("failedCount", totalFailed)
                .log("scheduler.complete");
    }

    private List<Long> processBatch(List<TrackedLink> links, Map<Long, List<Long>> chatIdsByLink) {
        List<Long> successIds = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (TrackedLink link : links) {
            List<Long> chatIds = chatIdsByLink.getOrDefault(link.getId(), List.of());
            futures.add(linkCheckExecutor.submit(() -> {
                try {
                    processLink(link, chatIds);
                    successIds.add(link.getId());
                } catch (Exception e) {
                    log.atError().addKeyValue("url", link.getUrl()).log("link.check.failed", e);
                    reportFailure(link, chatIds);
                }
            }));
        }
        awaitAll(futures);
        return successIds;
    }

    private void awaitAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.atError().log("scheduler.thread.interrupted", e);
            } catch (ExecutionException e) {
                log.atError().log("scheduler.thread.failed", e.getCause());
            }
        }
    }

    private void processLink(TrackedLink link, List<Long> chatIds) {
        Instant since = resolveSince(link);
        for (UpdateDescription desc : fetchUpdates(link.getUrl(), since)) {
            botClient.sendUpdate(new LinkUpdate(link.getId(), link.getUrl(), desc.format(link.getUrl()), chatIds));
            log.atInfo()
                    .addKeyValue("type", desc.getType())
                    .addKeyValue("url", link.getUrl())
                    .log("link.update.sent");
        }
    }

    private void reportFailure(TrackedLink link, List<Long> chatIds) {
        if (chatIds.isEmpty()) {
            return;
        }
        try {
            botClient.sendUpdate(new LinkUpdate(link.getId(), link.getUrl(), FAILURE_NOTICE, chatIds));
        } catch (Exception e) {
            log.atWarn().addKeyValue("url", link.getUrl()).log("link.failure.report.failed", e);
        }
    }

    private Instant resolveSince(TrackedLink link) {
        return link.getLastChecked() != null
                ? link.getLastChecked()
                : Instant.now().minus(schedulerProperties.getFallbackWindow());
    }

    private List<UpdateDescription> fetchUpdates(String url, Instant since) {
        Matcher githubMatcher = GITHUB_PATTERN.matcher(url);
        Matcher soMatcher = STACKOVERFLOW_PATTERN.matcher(url);
        if (githubMatcher.matches()) {
            return checkGitHub(githubMatcher.group(1), githubMatcher.group(2), since);
        }
        if (soMatcher.matches()) {
            return checkStackOverflow(Long.parseLong(soMatcher.group(1)), since);
        }
        log.atDebug().addKeyValue("url", url).log("link.unsupported");
        return List.of();
    }

    private List<UpdateDescription> checkGitHub(String owner, String repo, Instant since) {
        List<UpdateDescription> result = new ArrayList<>();
        for (IssueItem item : gitHubClient.getIssuesAndPullRequests(owner, repo, since)) {
            if (item.getCreatedAt() == null || !item.getCreatedAt().isAfter(since)) {
                continue;
            }
            UpdateDescription.Type type =
                    item.isPullRequest() ? UpdateDescription.Type.NEW_PR : UpdateDescription.Type.NEW_ISSUE;
            result.add(new UpdateDescription(
                    type,
                    null,
                    item.getTitle(),
                    item.getUser() != null ? item.getUser().getLogin() : "unknown",
                    item.getCreatedAt(),
                    PreviewUtil.truncate(item.getBody())));
        }
        return result;
    }

    private List<UpdateDescription> checkStackOverflow(long questionId, Instant since) {
        List<UpdateDescription> result = new ArrayList<>();

        String questionTitle = stackOverflowClient
                .getQuestion(questionId)
                .map(QuestionItem::getTitle)
                .orElse(null);

        for (AnswerItem answer : stackOverflowClient.getNewAnswers(questionId, since)) {
            String username = answer.getOwner() != null ? answer.getOwner().getDisplayName() : "unknown";
            Instant createdAt =
                    answer.getCreationDate() != null ? Instant.ofEpochSecond(answer.getCreationDate()) : Instant.now();
            result.add(new UpdateDescription(
                    UpdateDescription.Type.NEW_ANSWER,
                    questionTitle,
                    null,
                    username,
                    createdAt,
                    PreviewUtil.truncate(answer.getBody())));
        }

        for (CommentItem comment : stackOverflowClient.getNewComments(questionId, since)) {
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
                    PreviewUtil.truncate(comment.getBody())));
        }

        return result;
    }
}
