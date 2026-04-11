package backend.academy.linktracker.scrapper.repository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChatRepository implements ChatRepository {

    private final Set<Long> registeredChats = ConcurrentHashMap.newKeySet();

    @Override
    public void register(long chatId) {
        registeredChats.add(chatId);
    }

    @Override
    public boolean exists(long chatId) {
        return registeredChats.contains(chatId);
    }

    @Override
    public void remove(long chatId) {
        registeredChats.remove(chatId);
    }
}
