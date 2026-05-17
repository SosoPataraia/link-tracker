package backend.academy.linktracker.scrapper.ratelimit;

import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.atWarn()
                    .addKeyValue("ip", ip)
                    .addKeyValue("uri", request.getRequestURI())
                    .log("rate.limit.exceeded");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Too many requests\"}");
        }
    }

    private Bucket newBucket(String ignored) {
        var bandwidth = Bandwidth.builder()
                .capacity(rateLimitProperties.getCapacity())
                .refillGreedy(rateLimitProperties.getRefillTokens(), rateLimitProperties.getRefillPeriod())
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
