package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlChatRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlLinkRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlTagRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlRepositoryConfiguration {

    @Bean
    public ChatRepository chatRepository(JdbcClient jdbcClient) {
        return new SqlChatRepository(jdbcClient);
    }

    @Bean
    public LinkRepository linkRepository(JdbcClient jdbcClient) {
        return new SqlLinkRepository(jdbcClient);
    }

    @Bean
    public TagRepository tagRepository(JdbcClient jdbcClient) {
        return new SqlTagRepository(jdbcClient);
    }
}
