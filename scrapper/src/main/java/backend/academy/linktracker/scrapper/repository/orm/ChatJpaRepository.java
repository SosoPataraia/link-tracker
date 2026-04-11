package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.jpa.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatJpaRepository extends JpaRepository<ChatEntity, Long> {
}
