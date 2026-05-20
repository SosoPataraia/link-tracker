package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrmLinkRepositoryTest extends BaseOrmRepositoryTest {

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    LinkRepository linkRepository;

    @Test
    void save_andFindByChatAndUrl() {
        chatRepository.register(1L);
        var link = link(1L, "https://github.com/user/repo", List.of("work"));

        linkRepository.save(link);

        var found = linkRepository.findByChatAndUrl(1L, "https://github.com/user/repo");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getTags()).containsExactly("work");
    }

    @Test
    void remove_deletesLink() {
        chatRepository.register(1L);
        linkRepository.save(link(1L, "https://github.com/user/repo", List.of()));

        linkRepository.remove(1L, "https://github.com/user/repo");

        assertThat(linkRepository.findByChatAndUrl(1L, "https://github.com/user/repo"))
                .isEmpty();
    }

    @Test
    void findAllByChat_returnsOnlyOwnLinks() {
        chatRepository.register(1L);
        chatRepository.register(2L);
        linkRepository.save(link(1L, "https://github.com/user/repo", List.of()));
        linkRepository.save(link(2L, "https://github.com/other/repo", List.of()));

        assertThat(linkRepository.findAllByChat(1L)).hasSize(1);
        assertThat(linkRepository.findAllByChat(2L)).hasSize(1);
    }

    @Test
    void removeAllByChat_cleansUp() {
        chatRepository.register(1L);
        linkRepository.save(link(1L, "https://github.com/user/repo", List.of()));
        linkRepository.save(link(1L, "https://stackoverflow.com/questions/1/q", List.of()));

        linkRepository.removeAllByChat(1L);

        assertThat(linkRepository.findAllByChat(1L)).isEmpty();
    }

    private TrackedLink link(long chatId, String url, List<String> tags) {
        var l = new TrackedLink();
        l.setChatId(chatId);
        l.setUrl(url);
        l.setTags(tags);
        l.setLastChecked(Instant.now());
        l.setLastUpdated(Instant.now());
        return l;
    }
}
