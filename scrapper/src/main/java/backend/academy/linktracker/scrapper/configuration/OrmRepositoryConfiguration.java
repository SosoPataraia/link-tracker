package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.repository.orm.ChatJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.LinkJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmTagRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
public class OrmRepositoryConfiguration {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public ChatRepository chatRepository(ChatJpaRepository chatJpaRepository) {
        return new OrmChatRepository(chatJpaRepository);
    }

    @Bean
    public LinkRepository linkRepository(LinkJpaRepository linkJpaRepository, ChatJpaRepository chatJpaRepository) {
        return new OrmLinkRepository(linkJpaRepository, chatJpaRepository);
    }

    @Bean
    public TagRepository tagRepository() {
        return new OrmTagRepository(entityManager);
    }
}
