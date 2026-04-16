package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;

public interface NotificationSender {
    void send(LinkUpdate update);
}
