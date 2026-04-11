package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.jpa.LinkEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkJpaRepository extends JpaRepository<LinkEntity, Long> {

    Optional<LinkEntity> findByUrl(String url);

    @Query("""
            SELECT l FROM LinkEntity l
            JOIN l.chats c
            WHERE c.id = :chatId
            """)
    java.util.List<LinkEntity> findAllByChatsId(long chatId);

    @Query("""
            SELECT l FROM LinkEntity l
            JOIN l.chats c
            WHERE c.id = :chatId AND l.url = :url
            """)
    Optional<LinkEntity> findByChatIdAndUrl(long chatId, String url);
}
