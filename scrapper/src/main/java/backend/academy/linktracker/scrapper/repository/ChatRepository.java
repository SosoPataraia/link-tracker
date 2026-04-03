package backend.academy.linktracker.scrapper.repository;

public interface ChatRepository {
    void register(long chatId);

    boolean exists(long chatId);

    void remove(long chatId);
}
