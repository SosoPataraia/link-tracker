package backend.academy.linktracker.scrapper.cache;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkApiService;
import com.redis.testcontainers.RedisContainer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Import(TestcontainersConfiguration.class)
@org.junit.jupiter.api.Disabled("Runs locally; excluded from CI due to 10-minute job timeout")
class LinkCacheIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer("valkey/valkey:8-alpine");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    LinkApiService linkApiService;

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    CacheManager cacheManager;

    private static final AtomicLong chatIdSeq = new AtomicLong(99000000);
    private long chatId;

    @BeforeEach
    void setUp() {
        chatId = chatIdSeq.incrementAndGet();
        chatRepository.register(chatId);
        cacheManager.getCache("links").clear();
    }

    @Test
    void getLinks_cacheReturnsSameResult() {
        var first = linkApiService.getLinks(chatId);
        assertThat(first).isPresent();

        var second = linkApiService.getLinks(chatId);
        assertThat(second).isPresent();
        assertThat(second.orElseThrow().getSize()).isEqualTo(first.orElseThrow().getSize());
    }

    @Test
    void addLink_cacheEvictedAndReflectsNewData() {
        assertThat(linkApiService.getLinks(chatId).orElseThrow().getSize()).isEqualTo(0);
        linkApiService.addLink(
                chatId, new AddLinkRequest("https://stackoverflow.com/questions/123", List.of(), List.of()));
        assertThat(linkApiService.getLinks(chatId).orElseThrow().getSize()).isEqualTo(1);
    }

    @Test
    void removeLink_cacheEvictedAndReflectsRemoval() {
        linkApiService.addLink(chatId, new AddLinkRequest("https://github.com/test/repo", List.of(), List.of()));
        assertThat(linkApiService.getLinks(chatId).orElseThrow().getSize()).isEqualTo(1);
        assertThat(linkApiService.getLinks(chatId).orElseThrow().getSize()).isEqualTo(1);
        linkApiService.removeLink(chatId, new RemoveLinkRequest("https://github.com/test/repo"));
        assertThat(linkApiService.getLinks(chatId).orElseThrow().getSize()).isEqualTo(0);
    }
}
