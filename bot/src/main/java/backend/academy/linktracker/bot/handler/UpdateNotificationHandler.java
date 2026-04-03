package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.dto.LinkUpdate;

public interface UpdateNotificationHandler {
    void handleUpdate(LinkUpdate update);
}
