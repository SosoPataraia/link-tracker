package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.cache.client-side-enabled", havingValue = "true")
public class CaffeineLocalLinksCache implements LocalLinksCache {

    private static final String KEY_PREFIX = LinksCache.KEY_PREFIX;

    private final Cache<Long, ListLinksResponse> store;

    public CaffeineLocalLinksCache(CacheProperties props) {
        this.store = Caffeine.newBuilder()
                .maximumSize(props.getMaxSize())
                .expireAfterWrite(props.getTtl())
                .build();
    }

    @Override
    public Optional<ListLinksResponse> get(long chatId) {
        return Optional.ofNullable(store.getIfPresent(chatId));
    }

    @Override
    public void put(long chatId, ListLinksResponse value) {
        store.put(chatId, value);
    }

    @Override
    public void evict(long chatId) {
        store.invalidate(chatId);
        log.atDebug().addKeyValue("chatId", chatId).log("local.cache.evicted");
    }

    public void cleanUp() {
        store.cleanUp();
    }
}
