package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.cache.LocalLinksCache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.ClientSideCaching;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cache.client-side-enabled", havingValue = "true")
public class ClientSideCacheConfiguration {

    private final LettuceConnectionFactory lettuceConnectionFactory;
    private final LocalLinksCache localLinksCache;

    private StatefulRedisConnection<String, String> connection;
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    @PostConstruct
    public void enableClientTracking() {
        try {
            RedisClient redisClient = (RedisClient) lettuceConnectionFactory.getNativeClient();

            connection = redisClient.connect();
            pubSubConnection = redisClient.connectPubSub();

            ClientSideCaching.enable(
                CacheAccessor.forMap(new java.util.concurrent.ConcurrentHashMap<>()),
                connection,
                TrackingArgs.Builder.enabled().bcast()
            );

            pubSubConnection.addListener(new io.lettuce.core.pubsub.RedisPubSubAdapter<>() {
                @Override
                public void message(String channel, String message) {
                    log.debug("Invalidation message received for key={}", message);
                    try {
                        long chatId = Long.parseLong(message.replaceAll(".*links::", ""));
                        localLinksCache.evict(chatId);
                    } catch (NumberFormatException e) {
                        localLinksCache.evictAll();
                    }
                }
            });

            pubSubConnection.sync().subscribe("__redis__:invalidate");
            log.info("Client-side caching enabled via Lettuce CLIENT TRACKING");
        } catch (Exception e) {
            log.warn("Could not enable client-side caching: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (connection != null) connection.close();
        if (pubSubConnection != null) pubSubConnection.close();
    }
}
