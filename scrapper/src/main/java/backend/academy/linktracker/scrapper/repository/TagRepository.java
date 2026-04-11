package backend.academy.linktracker.scrapper.repository;

import java.util.List;

public interface TagRepository {

    void addTag(long linkId, long chatId, String tag);

    void removeTag(long linkId, long chatId, String tag);

    void removeAllTags(long linkId, long chatId);

    List<String> findTags(long linkId, long chatId);

    List<Long> findLinkIdsByTag(long chatId, String tag);
}
