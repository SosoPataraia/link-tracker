package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.jpa.ChatEntity;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrmChatRepository implements ChatRepository {

    private final ChatJpaRepository jpaRepository;

    @Override
    public void register(long chatId) {
        if (!jpaRepository.existsById(chatId)) {
            var entity = new ChatEntity();
            entity.setId(chatId);
            entity.setCreatedAt(Instant.now());
            jpaRepository.save(entity);
        }
    }

    @Override
    public boolean exists(long chatId) {
        return jpaRepository.existsById(chatId);
    }

    @Override
    public void remove(long chatId) {
        jpaRepository.deleteById(chatId);
    }
}
