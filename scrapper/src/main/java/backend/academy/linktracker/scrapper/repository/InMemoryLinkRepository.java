package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository implements LinkRepository {

    private final Map<Long, TrackedLink> linksById = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> linksByChatId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public TrackedLink save(TrackedLink link) {
        if (link.getId() == null) {
            link.setId(idSequence.getAndIncrement());
        }
        linksById.put(link.getId(), link);
        linksByChatId.computeIfAbsent(link.getChatId(), k -> new ArrayList<>()).add(link.getId());
        return link;
    }

    public Optional<TrackedLink> findById(long id) {
        return Optional.ofNullable(linksById.get(id));
    }

    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        List<Long> ids = linksByChatId.getOrDefault(chatId, List.of());
        return ids.stream()
                .map(linksById::get)
                .filter(l -> l != null && l.getUrl().equals(url))
                .findFirst();
    }

    public List<TrackedLink> findAllByChat(long chatId) {
        List<Long> ids = linksByChatId.getOrDefault(chatId, List.of());
        return ids.stream().map(linksById::get).filter(l -> l != null).toList();
    }

    public Collection<TrackedLink> findAll() {
        return linksById.values();
    }

    public boolean remove(long chatId, String url) {
        Optional<TrackedLink> link = findByChatAndUrl(chatId, url);
        if (link.isEmpty()) {
            return false;
        }
        long linkId = link.orElseThrow().getId();
        linksById.remove(linkId);
        List<Long> ids = linksByChatId.get(chatId);
        if (ids != null) {
            ids.remove(linkId);
        }
        return true;
    }

    public boolean existsChat(long chatId) {
        return linksByChatId.containsKey(chatId);
    }

    public void removeAllByChat(long chatId) {
        List<Long> ids = linksByChatId.remove(chatId);
        if (ids != null) {
            ids.forEach(linksById::remove);
        }
    }
}
