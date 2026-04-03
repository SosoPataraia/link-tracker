package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.UserSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySessionRepository implements SessionRepository {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    @Override
    public UserSession getOrCreate(long chatId) {
        return sessions.computeIfAbsent(chatId, k -> new UserSession());
    }

    @Override
    public UserSession get(long chatId) {
        return sessions.getOrDefault(chatId, new UserSession());
    }
}
