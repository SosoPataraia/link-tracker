package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotClientImpl implements BotClient {

    private static final String BOT_CLIENT_NAME = "botClient";

    private final RestClient botRestClient;

    @Override
    @Retry(name = BOT_CLIENT_NAME)
    public void sendUpdate(LinkUpdate update) {
        try {
            botRestClient
                    .post()
                    .uri("/updates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(update)
                    .retrieve()
                    .toBodilessEntity();
            log.atInfo()
                    .addKeyValue("url", update.getUrl())
                    .addKeyValue("chatIds", update.getTgChatIds())
                    .log("bot.update.sent");
        } catch (RestClientException e) {
            log.atError()
                    .addKeyValue("url", update.getUrl())
                    .addKeyValue("error", e.getMessage())
                    .log("bot.update.failed");
            throw e;
        }
    }
}
