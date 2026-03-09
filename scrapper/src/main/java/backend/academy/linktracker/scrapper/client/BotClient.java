package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotClient {

    private final RestClient botRestClient;

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
