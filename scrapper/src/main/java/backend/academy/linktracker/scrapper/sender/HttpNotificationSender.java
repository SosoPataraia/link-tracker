package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class HttpNotificationSender implements NotificationSender {

    private final BotClient botClient;

    @Override
    public void send(LinkUpdate update) {
        log.atInfo()
                .addKeyValue("url", update.getUrl())
                .addKeyValue("chatIds", update.getTgChatIds())
                .log("notification.http.send");
        botClient.sendUpdate(update);
    }
}
