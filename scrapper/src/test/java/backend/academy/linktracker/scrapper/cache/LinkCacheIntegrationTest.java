package backend.academy.linktracker.scrapper.cache;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalLinksCacheTest {

    CaffeineLocalLinksCache cache;

    @BeforeEach
    void setUp() {
        CacheProperties props = new CacheProperties();
        props.setTtl(Duration.ofSeconds(60));
        props.setMaxSize(1000);
        cache = new CaffeineLocalLinksCache(props);
    }

    @Test
    void get_miss_returnsEmpty() {
        assertThat(cache.get(1L)).isEmpty();
    }

    @Test
    void put_thenGet_returnsValue() {
        var response = new ListLinksResponse(List.of(new LinkResponse(1L, "https://github.com/u/r", List.of())), 1);

        cache.put(1L, response);

        assertThat(cache.get(1L)).isPresent();
        assertThat(cache.get(1L).get()).usingRecursiveComparison().isEqualTo(response);
    }

    @Test
    void evict_removesEntry() {
        cache.put(1L, new ListLinksResponse(List.of(), 0));
        cache.evict(1L);
        assertThat(cache.get(1L)).isEmpty();
    }

    @Test
    void evict_nonExistent_doesNotThrow() {
        cache.evict(999L);
    }

    @Test
    void maxSize_respected() {
        CacheProperties props = new CacheProperties();
        props.setTtl(Duration.ofSeconds(60));
        props.setMaxSize(2);
        var smallCache = new CaffeineLocalLinksCache(props);

        smallCache.put(1L, new ListLinksResponse(List.of(), 0));
        smallCache.put(2L, new ListLinksResponse(List.of(), 0));
        smallCache.put(3L, new ListLinksResponse(List.of(), 0));

        smallCache.cleanUp();

        long present = List.of(1L, 2L, 3L).stream()
                .filter(id -> smallCache.get(id).isPresent())
                .count();
        assertThat(present).isLessThanOrEqualTo(2);
    }
}
