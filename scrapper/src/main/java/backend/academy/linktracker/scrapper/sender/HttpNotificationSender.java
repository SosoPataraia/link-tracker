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
        log.info("Sending update via HTTP to bot: url={}", update.getUrl());
        botClient.sendUpdate(update);
    }
}
