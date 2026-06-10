package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.ClientSideCaching;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.cache.client-side-enabled", havingValue = "true")
public class ClientSideLinksCache implements LinksCache {

    private static final String KEY_PREFIX = LinksCache.KEY_PREFIX;

    private final LocalLinksCache localCache;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final LettuceConnectionFactory lettuceConnectionFactory;

    private StatefulRedisConnection<String, String> trackingConnection;

    public ClientSideLinksCache(
            LocalLinksCache localCache,
            ObjectMapper objectMapper,
            CacheProperties cacheProperties,
            LettuceConnectionFactory lettuceConnectionFactory) {
        this.localCache = localCache;
        this.objectMapper = objectMapper;
        this.cacheProperties = cacheProperties;
        this.lettuceConnectionFactory = lettuceConnectionFactory;
    }

    @PostConstruct
    public void enableTracking() {
        try {
            RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();
            trackingConnection = redisClient.connect();

            ClientSideCaching.enable(
                    CacheAccessor.forMap(new java.util.concurrent.ConcurrentHashMap<>()),
                    trackingConnection,
                    TrackingArgs.Builder.enabled().bcast().prefixes(KEY_PREFIX));

            trackingConnection.addListener(message -> {
                if (!"invalidate".equals(message.getType())) {
                    return;
                }
                if (message.getContent() == null) {
                    return;
                }
                for (Object key : message.getContent()) {
                    String keyStr = key.toString();
                    if (!keyStr.startsWith(KEY_PREFIX)) {
                        continue;
                    }
                    try {
                        long chatId = Long.parseLong(keyStr.substring(KEY_PREFIX.length()));
                        localCache.evict(chatId);
                        log.atDebug().addKeyValue("chatId", chatId).log("client.side.cache.invalidated");
                    } catch (NumberFormatException e) {
                        log.atDebug().addKeyValue("key", keyStr).log("client.side.cache.invalidate.key.skipped");
                    }
                }
            });

            log.atInfo().log("client.side.caching.enabled");
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("client.side.caching.init.failed");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (trackingConnection != null) {
            trackingConnection.close();
        }
    }

    @Override
    public Optional<ListLinksResponse> get(long chatId) {
        Optional<ListLinksResponse> local = localCache.get(chatId);
        if (local.isPresent()) {
            log.atDebug().addKeyValue("chatId", chatId).log("l1.cache.hit");
            return local;
        }

        String json = trackingConnection.sync().get(KEY_PREFIX + chatId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            ListLinksResponse value = objectMapper.readValue(json, ListLinksResponse.class);
            localCache.put(chatId, value);
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            log.atWarn()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log("client.side.cache.deserialize.failed");
            return Optional.empty();
        }
    }

    @Override
    public void put(long chatId, ListLinksResponse value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            trackingConnection
                    .sync()
                    .setex(KEY_PREFIX + chatId, cacheProperties.getTtl().getSeconds(), json);
            localCache.put(chatId, value);
        } catch (JsonProcessingException e) {
            log.atWarn()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log("client.side.cache.serialize.failed");
        }
    }

    @Override
    public void evict(long chatId) {
        trackingConnection.sync().del(KEY_PREFIX + chatId);
        localCache.evict(chatId);
    }
}
