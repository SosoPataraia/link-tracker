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
import backend.academy.linktracker.scrapper.repository.BaseRepositoryTest;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

class LinkCheckerServiceTest extends BaseRepositoryTest {

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    ChatRepository chatRepository;

    BotClient botClient;
    GitHubClient gitHubClient;
    StackOverflowClient stackOverflowClient;
    LinkCheckerService service;

    @BeforeEach
    void setUpService() {
        botClient = Mockito.mock(BotClient.class);
        gitHubClient = Mockito.mock(GitHubClient.class);
        stackOverflowClient = Mockito.mock(StackOverflowClient.class);
        service = new LinkCheckerService(linkRepository, gitHubClient, stackOverflowClient, botClient);
    }

    private TrackedLink saveLink(long chatId, String url, Instant lastUpdated) {
        chatRepository.register(chatId);
        var link = new TrackedLink();
        link.setChatId(chatId);
        link.setUrl(url);
        link.setTags(List.of());
        link.setLastChecked(Instant.now());
        link.setLastUpdated(lastUpdated);
        return linkRepository.save(link);
    }

    @Test
    void checksGithubLinksAndNotifiesOnUpdate() {
        var oldTime = Instant.parse("2024-01-01T00:00:00Z");
        var newTime = Instant.parse("2024-01-15T00:00:00Z");

        saveLink(100L, "https://github.com/user/repo", oldTime);

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
        saveLink(100L, "https://github.com/user/repo", time);

        when(gitHubClient.getLastUpdated("user", "repo")).thenReturn(time);

        service.checkAllLinks();

        verify(botClient, never()).sendUpdate(any());
    }

    @Test
    void notifiesOnlySubscribedUsers() {
        var oldTime = Instant.parse("2024-01-01T00:00:00Z");
        var newTime = Instant.parse("2024-01-15T00:00:00Z");

        saveLink(100L, "https://github.com/user/repo", oldTime);
        saveLink(999L, "https://github.com/other/other", oldTime);

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

        saveLink(100L, "https://stackoverflow.com/questions/12345/how-to-test", oldTime);

        when(stackOverflowClient.getLastActivity(12345L)).thenReturn(newTime);

        service.checkAllLinks();

        verify(botClient).sendUpdate(any());
    }

    @Test
    void handlesApiErrorGracefully() {
        saveLink(100L, "https://github.com/user/repo", Instant.parse("2024-01-01T00:00:00Z"));

        when(gitHubClient.getLastUpdated(anyString(), anyString())).thenReturn(null);

        service.checkAllLinks();
        verify(botClient, never()).sendUpdate(any());
    }
}
