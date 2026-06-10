package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.AnswerItem;
import backend.academy.linktracker.scrapper.dto.stackoverflow.CommentItem;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.InMemoryChatRepository;
import backend.academy.linktracker.scrapper.service.LinkService;
import backend.academy.linktracker.scrapper.service.LinkServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    GitHubClient gitHubClient;

    @Mock
    StackOverflowClient stackOverflowClient;

    @Mock
    BotClient botClient;

    InMemoryLinkRepository linkRepository;
    LinkService service;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepository();
        var props = new SchedulerProperties();
        service = new LinkServiceImpl(
                linkRepository,
                new InMemoryChatRepository(),
                gitHubClient,
                stackOverflowClient,
                botClient,
                props,
                Executors.newSingleThreadExecutor());
    }

    @Test
    void newIssue_sendsUpdateWithCorrectChatId() {
        linkRepository.save(link(100L, "https://github.com/user/repo"));

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenReturn(List.of(issueItem("Bug in login", "alice", Instant.now(), "Stack trace", false)));

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("https://github.com/user/repo");
        assertThat(captor.getValue().getTgChatIds()).containsExactly(100L);
    }

    @Test
    void newPR_sendsUpdateWithCorrectChatId() {
        linkRepository.save(link(100L, "https://github.com/user/repo"));

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenReturn(List.of(issueItem("Add dark mode", "bob", Instant.now(), "desc", true)));

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getDescription()).contains("New Pull Request");
    }

    @Test
    void noNewActivity_doesNotNotify() {
        linkRepository.save(link(100L, "https://github.com/user/repo"));

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenReturn(List.of());

        service.checkAllLinks();

        verify(botClient, never()).sendUpdate(any());
    }

    @Test
    void notifiesOnlySubscribedUsers() {
        linkRepository.save(link(100L, "https://github.com/user/repo"));
        linkRepository.save(link(999L, "https://github.com/other/other"));

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    String owner = inv.getArgument(0);
                    if ("user".equals(owner)) {
                        return List.of(issueItem("Issue", "alice", Instant.now(), "body", false));
                    }
                    return List.of();
                });

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getTgChatIds()).containsExactly(100L);
        assertThat(captor.getValue().getTgChatIds()).doesNotContain(999L);
    }

    @Test
    void newAnswer_sendsUpdateForStackOverflow() {
        linkRepository.save(link(100L, "https://stackoverflow.com/questions/12345/how-to-test"));

        when(stackOverflowClient.getQuestion(12345L)).thenReturn(Optional.empty());
        when(stackOverflowClient.getNewAnswers(anyLong(), any()))
                .thenReturn(List.of(answerItem("charlie", 1705312200L, "Use JUnit 5")));
        when(stackOverflowClient.getNewComments(anyLong(), any())).thenReturn(List.of());

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getDescription()).contains("New Answer");
        assertThat(captor.getValue().getDescription()).contains("charlie");
    }

    @Test
    void newComment_sendsUpdateForStackOverflow() {
        linkRepository.save(link(100L, "https://stackoverflow.com/questions/12345/how-to-test"));

        when(stackOverflowClient.getQuestion(12345L)).thenReturn(Optional.empty());
        when(stackOverflowClient.getNewAnswers(anyLong(), any())).thenReturn(List.of());
        when(stackOverflowClient.getNewComments(anyLong(), any()))
                .thenReturn(List.of(commentItem("dave", 1705312200L, "Thanks!")));

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getDescription()).contains("New Comment");
    }

    @Test
    void lastChecked_isUpdatedAfterCheck() {
        var saved = linkRepository.save(link(100L, "https://github.com/user/repo"));

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenReturn(List.of());

        service.checkAllLinks();

        var updated = linkRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getLastChecked()).isNotNull();
    }

    @Test
    void apiError_doesNotAdvanceLastChecked_andSendsFailureNotice() {
        var saved = linkRepository.save(link(100L, "https://github.com/user/repo"));
        Instant before = saved.getLastChecked();

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("API down"));

        service.checkAllLinks();

        var updated = linkRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getLastChecked()).isEqualTo(before);
        verify(botClient).sendUpdate(any());
    }

    private TrackedLink link(long chatId, String url) {
        var l = new TrackedLink();
        l.setChatId(chatId);
        l.setUrl(url);
        l.setTags(List.of());
        l.setLastChecked(Instant.EPOCH);
        l.setLastUpdated(Instant.now());
        return l;
    }

    private IssueItem issueItem(String title, String login, Instant createdAt, String body, boolean isPr) {
        var item = new IssueItem();
        item.setTitle(title);
        item.setCreatedAt(createdAt);
        item.setBody(body);
        if (isPr) {
            item.setPullRequest(new Object());
        }
        var user = new IssueItem.UserInfo();
        user.setLogin(login);
        item.setUser(user);
        return item;
    }

    private AnswerItem answerItem(String displayName, long creationDate, String body) {
        var item = new AnswerItem();
        item.setCreationDate(creationDate);
        item.setBody(body);
        var owner = new AnswerItem.OwnerInfo();
        owner.setDisplayName(displayName);
        item.setOwner(owner);
        return item;
    }

    private CommentItem commentItem(String displayName, long creationDate, String body) {
        var item = new CommentItem();
        item.setCreationDate(creationDate);
        item.setBody(body);
        var owner = new CommentItem.OwnerInfo();
        owner.setDisplayName(displayName);
        item.setOwner(owner);
        return item;
    }
}
