package backend.academy.linktracker.scrapper;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

class InMemoryLinkRepository implements LinkRepository {

    private final Map<Long, TrackedLink> linksById = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> linksByChatId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public TrackedLink save(TrackedLink link) {
        if (link.getId() == null) {
            link.setId(idSequence.getAndIncrement());
        }
        linksById.put(link.getId(), link);
        linksByChatId.computeIfAbsent(link.getChatId(), k -> new ArrayList<>()).add(link.getId());
        return link;
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return Optional.ofNullable(linksById.get(id));
    }

    @Override
    public Optional<TrackedLink> findByChatAndUrl(long chatId, String url) {
        return linksByChatId.getOrDefault(chatId, List.of()).stream()
                .map(linksById::get)
                .filter(l -> l != null && l.getUrl().equals(url))
                .findFirst();
    }

    @Override
    public List<TrackedLink> findAllByChat(long chatId) {
        return linksByChatId.getOrDefault(chatId, List.of()).stream()
                .map(linksById::get)
                .filter(l -> l != null)
                .toList();
    }

    @Override
    public Collection<TrackedLink> findAll() {
        return linksById.values();
    }

    @Override
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

    @Override
    public void removeAllByChat(long chatId) {
        List<Long> ids = linksByChatId.remove(chatId);
        if (ids != null) {
            ids.forEach(linksById::remove);
        }
    }

    @Override
    public void updateLastChecked(Collection<Long> linkIds, Instant lastChecked) {
        linkIds.forEach(id -> {
            TrackedLink link = linksById.get(id);
            if (link != null) {
                link.setLastChecked(lastChecked);
            }
        });
    }

    @Override
    public List<TrackedLink> findLinksToCheck(
            Instant checkedBefore, Instant cursorLastChecked, long cursorId, int limit) {
        Instant epoch = Instant.EPOCH;
        return linksById.values().stream()
                .filter(l -> {
                    Instant lc = l.getLastChecked() != null ? l.getLastChecked() : epoch;
                    return lc.isBefore(checkedBefore)
                            && (lc.isAfter(cursorLastChecked)
                                    || (lc.equals(cursorLastChecked) && l.getId() > cursorId));
                })
                .sorted(Comparator.comparing((TrackedLink l) -> l.getLastChecked() != null ? l.getLastChecked() : epoch)
                        .thenComparingLong(TrackedLink::getId))
                .limit(limit)
                .toList();
    }

    @Override
    public Map<Long, List<Long>> findChatIdsByLinkIds(Collection<Long> linkIds) {
        return linkIds.stream()
                .filter(linksById::containsKey)
                .collect(Collectors.toMap(
                        id -> id, id -> List.of(linksById.get(id).getChatId())));
    }
}
