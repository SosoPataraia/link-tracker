package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bot.transport", havingValue = "rest", matchIfMissing = true)
public class BotClientImpl implements BotClient {

    private final RestClient botRestClient;

    @Override
    public void sendUpdate(LinkUpdate update) {
        try {
            botRestClient
                    .post()
                    .uri("/updates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(update)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent update to bot for url={} chatIds={}", update.getUrl(), update.getTgChatIds());
        } catch (RestClientException e) {
            log.error("Failed to send update to bot for url={}: {}", update.getUrl(), e.getMessage());
        }
    }
}
