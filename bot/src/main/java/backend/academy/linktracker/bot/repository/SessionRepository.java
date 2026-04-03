package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.UserSession;

public interface SessionRepository {
    UserSession getOrCreate(long chatId);

    UserSession get(long chatId);
}
