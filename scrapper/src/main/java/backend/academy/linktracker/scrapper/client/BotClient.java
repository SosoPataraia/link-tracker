package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;

public interface BotClient {

    /**
     * Sends an update to the bot. Propagates a {@link org.springframework.web.client.RestClientException}
     * if delivery fails, so the caller can avoid marking the link as checked.
     */
    void sendUpdate(LinkUpdate update);
}
