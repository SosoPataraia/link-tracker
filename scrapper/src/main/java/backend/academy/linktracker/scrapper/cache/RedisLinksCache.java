package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cache.client-side-enabled", havingValue = "false", matchIfMissing = true)
public class RedisLinksCache implements LinksCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;

    @Override
    public Optional<ListLinksResponse> get(long chatId) {
        String json = redisTemplate.opsForValue().get(LinksCache.key(chatId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ListLinksResponse.class));
        } catch (JsonProcessingException e) {
            log.atWarn()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log("cache.deserialize.failed");
            return Optional.empty();
        }
    }

    @Override
    public void put(long chatId, ListLinksResponse value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(LinksCache.key(chatId), json, cacheProperties.getTtl());
        } catch (JsonProcessingException e) {
            log.atWarn()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log("cache.serialize.failed");
        }
    }

    @Override
    public void evict(long chatId) {
        redisTemplate.delete(LinksCache.key(chatId));
    }
}
