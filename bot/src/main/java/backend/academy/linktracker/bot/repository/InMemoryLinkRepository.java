package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.TrackedLink;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository {

    // chatId -> list of tracked links
    private final Map<Long, List<TrackedLink>> storage = new ConcurrentHashMap<>();

    public List<TrackedLink> findAllByChat(long chatId) {
        return new ArrayList<>(storage.getOrDefault(chatId, new ArrayList<>()));
    }

    public List<TrackedLink> findByChatAndTag(long chatId, String tag) {
        return storage.getOrDefault(chatId, new ArrayList<>()).stream()
                .filter(link -> link.getTags().contains(tag))
                .toList();
    }

    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        return storage.getOrDefault(chatId, new ArrayList<>()).stream()
                .filter(link -> link.getUrl().equals(url))
                .findFirst();
    }

    public TrackedLink save(long chatId, TrackedLink link) {
        storage.computeIfAbsent(chatId, k -> new ArrayList<>());
        List<TrackedLink> links = storage.get(chatId);
        links.removeIf(l -> l.getUrl().equals(link.getUrl()));
        links.add(link);
        return link;
    }

    public boolean remove(long chatId, String url) {
        List<TrackedLink> links = storage.get(chatId);
        if (links == null) {
            return false;
        }
        return links.removeIf(l -> l.getUrl().equals(url));
    }

    public boolean exists(long chatId, String url) {
        return storage.getOrDefault(chatId, new ArrayList<>()).stream()
                .anyMatch(l -> l.getUrl().equals(url));
    }

    public Map<Long, List<TrackedLink>> findAll() {
        return new ConcurrentHashMap<>(storage);
    }
}
