package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.util.Optional;

public interface LinksCache {

    String KEY_PREFIX = "links::";

    static String key(long chatId) {
        return KEY_PREFIX + chatId;
    }

    Optional<ListLinksResponse> get(long chatId);

    void put(long chatId, ListLinksResponse value);

    void evict(long chatId);
}
