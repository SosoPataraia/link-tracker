package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalLinksCache {

    private final ConcurrentHashMap<Long, ListLinksResponse> store = new ConcurrentHashMap<>();

    public ListLinksResponse get(long chatId) {
        return store.get(chatId);
    }

    public void put(long chatId, ListLinksResponse response) {
        store.put(chatId, response);
    }

    public void evict(long chatId) {
        store.remove(chatId);
        log.atDebug().addKeyValue("chatId", chatId).log("cache.l1.evicted");
    }

    public void evictAll() {
        store.clear();
        log.atDebug().log("cache.l1.cleared");
    }
}
