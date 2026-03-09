package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.InMemoryLinkRepository;
import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkCheckerServiceTest {

    @Mock
    GitHubClient gitHubClient;

    @Mock
    StackOverflowClient stackOverflowClient;

    @Mock
    BotClient botClient;

    InMemoryLinkRepository linkRepository;
    LinkCheckerService service;

    @BeforeEach
    void setUp() {
        linkRepository = new InMemoryLinkRepository();
        service = new LinkCheckerService(linkRepository, gitHubClient, stackOverflowClient, botClient);
    }

    @Test
    void checksGithubLinksAndNotifiesOnUpdate() {
        var oldTime = Instant.parse("2024-01-01T00:00:00Z");
        var newTime = Instant.parse("2024-01-15T00:00:00Z");

        var link = new TrackedLink(null, 100L, "https://github.com/user/repo", List.of(), Instant.now(), oldTime);
        linkRepository.save(link);

        when(gitHubClient.getLastUpdated("user", "repo")).thenReturn(newTime);

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("https://github.com/user/repo");
        assertThat(captor.getValue().getTgChatIds()).contains(100L);
    }

    @Test
    void doesNotNotifyWhenNoUpdate() {
        var time = Instant.parse("2024-01-01T00:00:00Z");
        var link = new TrackedLink(null, 100L, "https://github.com/user/repo", List.of(), Instant.now(), time);
        linkRepository.save(link);

        when(gitHubClient.getLastUpdated("user", "repo")).thenReturn(time);

        service.checkAllLinks();

        verify(botClient, never()).sendUpdate(any());
    }

    @Test
    void notifiesOnlySubscribedUsers() {
        var oldTime = Instant.parse("2024-01-01T00:00:00Z");
        var newTime = Instant.parse("2024-01-15T00:00:00Z");

        // User 100 tracks the repo, user 200 does not
        linkRepository.save(
                new TrackedLink(null, 100L, "https://github.com/user/repo", List.of(), Instant.now(), oldTime));
        linkRepository.save(
                new TrackedLink(null, 999L, "https://github.com/other/other", List.of(), Instant.now(), oldTime));

        when(gitHubClient.getLastUpdated("user", "repo")).thenReturn(newTime);
        when(gitHubClient.getLastUpdated("other", "other")).thenReturn(oldTime);

        service.checkAllLinks();

        var captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botClient).sendUpdate(captor.capture());
        assertThat(captor.getValue().getTgChatIds()).containsExactly(100L);
        assertThat(captor.getValue().getTgChatIds()).doesNotContain(999L);
    }

    @Test
    void handlesStackOverflowLinks() {
        var oldTime = Instant.parse("2024-01-01T00:00:00Z");
        var newTime = Instant.parse("2024-01-15T00:00:00Z");

        linkRepository.save(new TrackedLink(
                null,
                100L,
                "https://stackoverflow.com/questions/12345/how-to-test",
                List.of(),
                Instant.now(),
                oldTime));

        when(stackOverflowClient.getLastActivity(12345L)).thenReturn(newTime);

        service.checkAllLinks();

        verify(botClient).sendUpdate(any());
    }

    @Test
    void handlesApiErrorGracefully() {
        linkRepository.save(new TrackedLink(
                null,
                100L,
                "https://github.com/user/repo",
                List.of(),
                Instant.now(),
                Instant.parse("2024-01-01T00:00:00Z")));

        when(gitHubClient.getLastUpdated(anyString(), anyString())).thenReturn(null);

        // Should not throw
        service.checkAllLinks();
        verify(botClient, never()).sendUpdate(any());
    }
}
