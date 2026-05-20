package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {

    TrackedLink save(TrackedLink link);

    Optional<TrackedLink> findById(long id);

    Optional<TrackedLink> findByChatAndUrl(long chatId, String url);

    List<TrackedLink> findAllByChat(long chatId);

    List<TrackedLink> findAllByChat(long chatId, int limit, int offset);

    Collection<TrackedLink> findAll();

    boolean remove(long chatId, String url);

    void removeAllByChat(long chatId);
}
