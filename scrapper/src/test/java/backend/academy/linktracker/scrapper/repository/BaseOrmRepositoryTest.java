package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("orm-test")
public abstract class BaseOrmRepositoryTest {

    @Autowired
    protected JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM link_tags").update();
        jdbcClient.sql("DELETE FROM link_chat").update();
        jdbcClient.sql("DELETE FROM links").update();
        jdbcClient.sql("DELETE FROM chats").update();
    }
}
