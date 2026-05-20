package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.jpa.ChatEntity;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmChatRepository implements ChatRepository {

    private final ChatJpaRepository jpaRepository;

    @Override
    @Transactional
    public void register(long chatId) {
        if (!jpaRepository.existsById(chatId)) {
            var entity = new ChatEntity();
            entity.setId(chatId);
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
