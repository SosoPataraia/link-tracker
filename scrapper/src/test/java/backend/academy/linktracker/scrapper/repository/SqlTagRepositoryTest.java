package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SqlTagRepositoryTest extends BaseRepositoryTest {

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    TagRepository tagRepository;

    @Test
    void addAndFindTags() {
        chatRepository.register(1L);
        var link = savedLink(1L, "https://github.com/user/repo");

        tagRepository.addTag(link.getId(), 1L, "work");
        tagRepository.addTag(link.getId(), 1L, "java");

        assertThat(tagRepository.findTags(link.getId(), 1L))
            .containsExactlyInAnyOrder("work", "java");
    }

    @Test
    void removeTag() {
        chatRepository.register(1L);
        var link = savedLink(1L, "https://github.com/user/repo");

        tagRepository.addTag(link.getId(), 1L, "work");
        tagRepository.addTag(link.getId(), 1L, "java");
        tagRepository.removeTag(link.getId(), 1L, "work");

        assertThat(tagRepository.findTags(link.getId(), 1L)).containsExactly("java");
    }

    @Test
    void removeAllTags() {
        chatRepository.register(1L);
        var link = savedLink(1L, "https://github.com/user/repo");

        tagRepository.addTag(link.getId(), 1L, "work");
        tagRepository.addTag(link.getId(), 1L, "java");
        tagRepository.removeAllTags(link.getId(), 1L);

        assertThat(tagRepository.findTags(link.getId(), 1L)).isEmpty();
    }

    @Test
    void findLinkIdsByTag() {
        chatRepository.register(1L);
        var link1 = savedLink(1L, "https://github.com/user/repo1");
        var link2 = savedLink(1L, "https://github.com/user/repo2");

        tagRepository.addTag(link1.getId(), 1L, "work");
        tagRepository.addTag(link2.getId(), 1L, "work");

        assertThat(tagRepository.findLinkIdsByTag(1L, "work"))
            .containsExactlyInAnyOrder(link1.getId(), link2.getId());
    }

    private TrackedLink savedLink(long chatId, String url) {
        var l = new TrackedLink();
        l.setChatId(chatId);
        l.setUrl(url);
        l.setTags(List.of());
        l.setLastChecked(Instant.now());
        l.setLastUpdated(Instant.now());
        return linkRepository.save(l);
    }
}
