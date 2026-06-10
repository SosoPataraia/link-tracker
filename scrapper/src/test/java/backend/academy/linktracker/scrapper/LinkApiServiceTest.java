package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.cache.LinksCache;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkApiService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkApiServiceTest {

    @Mock
    ChatRepository chatRepository;

    @Mock
    LinkRepository linkRepository;

    @Mock
    LinksCache linksCache;

    LinkApiService service;

    @BeforeEach
    void setUp() {
        service = new LinkApiService(linkRepository, chatRepository, linksCache);
    }

    @Test
    void getLinks_chatNotFound_throws() {
        when(chatRepository.exists(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.getLinks(1L)).isInstanceOf(ChatNotFoundException.class);
        verify(linksCache, never()).get(anyLong());
    }

    @Test
    void getLinks_cacheHit_doesNotQueryRepository() {
        when(chatRepository.exists(1L)).thenReturn(true);
        var cached = new ListLinksResponse(List.of(), 0);
        when(linksCache.get(1L)).thenReturn(Optional.of(cached));

        var result = service.getLinks(1L);

        assertThat(result).isSameAs(cached);
        verify(linkRepository, never()).findAllByChat(anyLong());
    }

    @Test
    void getLinks_cacheMiss_loadsAndPuts() {
        when(chatRepository.exists(1L)).thenReturn(true);
        when(linksCache.get(1L)).thenReturn(Optional.empty());
        when(linkRepository.findAllByChat(1L)).thenReturn(List.of(trackedLink(10L, "https://github.com/user/repo")));

        var result = service.getLinks(1L);

        assertThat(result.getSize()).isEqualTo(1);
        verify(linksCache).put(1L, result);
    }

    @Test
    void addLink_chatNotFound_throws() {
        when(chatRepository.exists(1L)).thenReturn(false);
        assertThatThrownBy(
                        () -> service.addLink(1L, new AddLinkRequest("https://github.com/u/r", List.of(), List.of())))
                .isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void addLink_duplicate_throws() {
        when(chatRepository.exists(1L)).thenReturn(true);
        when(linkRepository.findByChatAndUrl(1L, "https://github.com/u/r"))
                .thenReturn(Optional.of(trackedLink(10L, "https://github.com/u/r")));
        assertThatThrownBy(
                        () -> service.addLink(1L, new AddLinkRequest("https://github.com/u/r", List.of(), List.of())))
                .isInstanceOf(LinkAlreadyExistsException.class);
        verify(linksCache, never()).evict(anyLong());
    }

    @Test
    void addLink_happy_savesAndEvicts() {
        when(chatRepository.exists(1L)).thenReturn(true);
        when(linkRepository.findByChatAndUrl(1L, "https://github.com/u/r")).thenReturn(Optional.empty());
        when(linkRepository.save(any())).thenReturn(trackedLink(10L, "https://github.com/u/r"));

        var result = service.addLink(1L, new AddLinkRequest("https://github.com/u/r", List.of(), List.of()));

        assertThat(result.getUrl()).isEqualTo("https://github.com/u/r");
        verify(linksCache).evict(1L);
    }

    @Test
    void removeLink_chatNotFound_throws() {
        when(chatRepository.exists(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.removeLink(1L, new RemoveLinkRequest("https://github.com/u/r")))
                .isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void removeLink_linkNotFound_throws() {
        when(chatRepository.exists(1L)).thenReturn(true);
        when(linkRepository.findByChatAndUrl(1L, "https://github.com/u/r")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeLink(1L, new RemoveLinkRequest("https://github.com/u/r")))
                .isInstanceOf(LinkNotFoundException.class);
        verify(linksCache, never()).evict(anyLong());
    }

    @Test
    void removeLink_happy_removesAndEvicts() {
        when(chatRepository.exists(1L)).thenReturn(true);
        when(linkRepository.findByChatAndUrl(1L, "https://github.com/u/r"))
                .thenReturn(Optional.of(trackedLink(10L, "https://github.com/u/r")));

        var result = service.removeLink(1L, new RemoveLinkRequest("https://github.com/u/r"));

        assertThat(result.getId()).isEqualTo(10L);
        verify(linkRepository).remove(1L, "https://github.com/u/r");
        verify(linksCache).evict(1L);
    }

    private TrackedLink trackedLink(long id, String url) {
        var l = new TrackedLink();
        l.setId(id);
        l.setChatId(1L);
        l.setUrl(url);
        l.setTags(List.of());
        l.setLastChecked(Instant.now());
        l.setLastUpdated(Instant.now());
        return l;
    }
}
