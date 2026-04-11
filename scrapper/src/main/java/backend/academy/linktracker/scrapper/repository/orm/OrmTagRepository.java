package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.jpa.LinkTagEntity;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmTagRepository implements TagRepository {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void addTag(long linkId, long chatId, String tag) {
        var entity = new LinkTagEntity();
        entity.setLink(
                entityManager.getReference(backend.academy.linktracker.scrapper.model.jpa.LinkEntity.class, linkId));
        entity.setChatId(chatId);
        entity.setTag(tag);
        entityManager.persist(entity);
    }

    @Override
    @Transactional
    public void removeTag(long linkId, long chatId, String tag) {
        entityManager
                .createQuery("""
                DELETE FROM LinkTagEntity t
                WHERE t.link.id = :linkId AND t.chatId = :chatId AND t.tag = :tag
                """)
                .setParameter("linkId", linkId)
                .setParameter("chatId", chatId)
                .setParameter("tag", tag)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void removeAllTags(long linkId, long chatId) {
        entityManager
                .createQuery("""
                DELETE FROM LinkTagEntity t
                WHERE t.link.id = :linkId AND t.chatId = :chatId
                """)
                .setParameter("linkId", linkId)
                .setParameter("chatId", chatId)
                .executeUpdate();
    }

    @Override
    public List<String> findTags(long linkId, long chatId) {
        return entityManager
                .createQuery("""
                SELECT t.tag FROM LinkTagEntity t
                WHERE t.link.id = :linkId AND t.chatId = :chatId
                """, String.class)
                .setParameter("linkId", linkId)
                .setParameter("chatId", chatId)
                .getResultList();
    }

    @Override
    public List<Long> findLinkIdsByTag(long chatId, String tag) {
        return entityManager
                .createQuery("""
                SELECT t.link.id FROM LinkTagEntity t
                WHERE t.chatId = :chatId AND t.tag = :tag
                """, Long.class)
                .setParameter("chatId", chatId)
                .setParameter("tag", tag)
                .getResultList();
    }
}
