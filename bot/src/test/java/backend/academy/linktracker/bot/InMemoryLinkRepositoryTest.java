package backend.academy.linktracker.bot;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.repository.InMemoryLinkRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryLinkRepositoryTest {

    private InMemoryLinkRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryLinkRepository();
    }

    @Test
    void saveAndFindByChat() {
        var link = new TrackedLink("https://github.com/user/repo", List.of("work"));
        repository.save(1L, link);

        var found = repository.findAllByChat(1L);
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getUrl()).isEqualTo("https://github.com/user/repo");
    }

    @Test
    void findByChatAndTag_filtersCorrectly() {
        repository.save(1L, new TrackedLink("https://github.com/a/b", List.of("work", "java")));
        repository.save(1L, new TrackedLink("https://github.com/c/d", List.of("hobby")));

        var workLinks = repository.findByChatAndTag(1L, "work");
        assertThat(workLinks).hasSize(1);
        assertThat(workLinks.getFirst().getUrl()).isEqualTo("https://github.com/a/b");
    }

    @Test
    void remove_removesLink() {
        repository.save(1L, new TrackedLink("https://github.com/user/repo", List.of()));
        boolean removed = repository.remove(1L, "https://github.com/user/repo");

        assertThat(removed).isTrue();
        assertThat(repository.findAllByChat(1L)).isEmpty();
    }

    @Test
    void exists_returnsTrueForExistingLink() {
        repository.save(1L, new TrackedLink("https://github.com/user/repo", List.of()));
        assertThat(repository.exists(1L, "https://github.com/user/repo")).isTrue();
        assertThat(repository.exists(1L, "https://github.com/other/repo")).isFalse();
    }

    @Test
    void differentChatsAreIsolated() {
        repository.save(1L, new TrackedLink("https://github.com/user/repo", List.of()));
        repository.save(2L, new TrackedLink("https://github.com/other/repo", List.of()));

        assertThat(repository.findAllByChat(1L)).hasSize(1);
        assertThat(repository.findAllByChat(2L)).hasSize(1);
        assertThat(repository.findAllByChat(1L).getFirst().getUrl()).isEqualTo("https://github.com/user/repo");
    }
}
