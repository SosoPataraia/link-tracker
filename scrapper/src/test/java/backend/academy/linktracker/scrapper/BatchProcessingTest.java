package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.GitHubClient;
import backend.academy.linktracker.scrapper.client.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.github.IssueItem;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class BatchProcessingTest {

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    ChatRepository chatRepository;

    @MockitoBean
    BotClient botClient;

    @MockitoBean
    GitHubClient gitHubClient;

    @MockitoBean
    StackOverflowClient stackOverflowClient;

    @Autowired
    LinkService linkService;

    @Autowired
    org.springframework.jdbc.core.simple.JdbcClient jdbcClient;

    @BeforeEach
    void clean() {
        jdbcClient.sql("DELETE FROM link_tags").update();
        jdbcClient.sql("DELETE FROM link_chat").update();
        jdbcClient.sql("DELETE FROM links").update();
        jdbcClient.sql("DELETE FROM chats").update();
    }

    @Test
    void findLinksToCheck_returnsStalestLinksFirst() {
        chatRepository.register(1L);

        var old = savedLink(1L, "https://github.com/user/old-repo", Instant.parse("2024-01-01T00:00:00Z"));
        var mid = savedLink(1L, "https://github.com/user/mid-repo", Instant.parse("2024-06-01T00:00:00Z"));
        savedLink(1L, "https://github.com/user/new-repo", Instant.parse("2024-12-01T00:00:00Z"));

        List<TrackedLink> batch = linkRepository.findLinksToCheck(Instant.now(), Instant.EPOCH, 0L, 2);

        assertThat(batch).hasSize(2);
        assertThat(batch.get(0).getUrl()).isEqualTo(old.getUrl());
        assertThat(batch.get(1).getUrl()).isEqualTo(mid.getUrl());
    }

    @Test
    void findLinksToCheck_nullLastChecked_comesFirst() {
        chatRepository.register(1L);

        savedLink(1L, "https://github.com/user/checked-repo", Instant.parse("2024-01-01T00:00:00Z"));
        var nullChecked = savedLink(1L, "https://github.com/user/never-checked", null);

        List<TrackedLink> batch = linkRepository.findLinksToCheck(Instant.now(), Instant.EPOCH, 0L, 10);

        assertThat(batch.getFirst().getUrl()).isEqualTo(nullChecked.getUrl());
    }

    @Test
    void findLinksToCheck_keysetPagination_noOverlap() {
        chatRepository.register(1L);
        for (int i = 1; i <= 5; i++) {
            savedLink(1L, "https://github.com/user/repo-" + i, Instant.parse("2024-0" + i + "-01T00:00:00Z"));
        }

        List<TrackedLink> first = linkRepository.findLinksToCheck(Instant.now(), Instant.EPOCH, 0L, 3);
        assertThat(first).hasSize(3);

        TrackedLink last = first.getLast();
        Instant lastChecked = last.getLastChecked() != null ? last.getLastChecked() : Instant.EPOCH;
        List<TrackedLink> second = linkRepository.findLinksToCheck(Instant.now(), lastChecked, last.getId(), 3);
        assertThat(second).hasSize(2);

        var firstUrls = first.stream().map(TrackedLink::getUrl).toList();
        var secondUrls = second.stream().map(TrackedLink::getUrl).toList();
        assertThat(firstUrls).doesNotContainAnyElementsOf(secondUrls);
    }

    @Test
    void errorOnOneLink_doesNotPreventOthersFromBeingProcessed() throws InterruptedException {
        chatRepository.register(1L);
        chatRepository.register(2L);

        savedLink(1L, "https://github.com/user/failing-repo", Instant.EPOCH);
        savedLink(2L, "https://github.com/other/working-repo", Instant.EPOCH);

        when(gitHubClient.getIssuesAndPullRequests(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    String owner = inv.getArgument(0);
                    if ("user".equals(owner)) {
                        throw new RuntimeException("Simulated API failure");
                    }
                    return List.of(issueItem("Working issue", "carol"));
                });

        linkService.checkAllLinks();
        Thread.sleep(200);

        verify(botClient, atLeast(1)).sendUpdate(any());
    }

    @Test
    void subscriptionCreation_savesAllEntities() {
        chatRepository.register(42L);

        var link = new TrackedLink();
        link.setChatId(42L);
        link.setUrl("https://github.com/user/repo");
        link.setTags(List.of("work", "java"));
        link.setLastChecked(Instant.now());
        link.setLastUpdated(Instant.now());

        var saved = linkRepository.save(link);
        assertThat(saved.getId()).isNotNull();

        var found = linkRepository.findByChatAndUrl(42L, "https://github.com/user/repo");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getTags()).containsExactlyInAnyOrder("work", "java");
        assertThat(chatRepository.exists(42L)).isTrue();
    }

    private TrackedLink savedLink(long chatId, String url, Instant lastChecked) {
        var l = new TrackedLink();
        l.setChatId(chatId);
        l.setUrl(url);
        l.setTags(List.of());
        l.setLastChecked(lastChecked);
        l.setLastUpdated(Instant.now());
        return linkRepository.save(l);
    }

    private IssueItem issueItem(String title, String login) {
        var item = new IssueItem();
        item.setTitle(title);
        item.setCreatedAt(Instant.now());
        item.setBody("Some body text");
        var user = new IssueItem.UserInfo();
        user.setLogin(login);
        item.setUser(user);
        return item;
    }
}
