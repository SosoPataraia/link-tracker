package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SqlChatRepositoryTest extends BaseRepositoryTest {

    @Autowired
    ChatRepository chatRepository;

    @Test
    void register_andExists() {
        chatRepository.register(100L);
        assertThat(chatRepository.exists(100L)).isTrue();
    }

    @Test
    void remove_deletesChat() {
        chatRepository.register(100L);
        chatRepository.remove(100L);
        assertThat(chatRepository.exists(100L)).isFalse();
    }

    @Test
    void exists_returnsFalse_whenNotRegistered() {
        assertThat(chatRepository.exists(999L)).isFalse();
    }

    @Test
    void register_idempotent() {
        chatRepository.register(100L);
        chatRepository.register(100L); // should not throw
        assertThat(chatRepository.exists(100L)).isTrue();
    }
}
