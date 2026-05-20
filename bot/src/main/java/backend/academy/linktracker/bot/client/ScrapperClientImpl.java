package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapperClientImpl implements ScrapperClient {

    private final RestClient scrapperRestClient;

    @Override
    public void registerChat(long chatId) {
        try {
            scrapperRestClient.post().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.atError().addKeyValue("chatId", chatId).log("scrapper.chat.register.failed", e);
        }
    }

    @Override
    public void deleteChat(long chatId) {
        try {
            scrapperRestClient.delete().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.atError().addKeyValue("chatId", chatId).log("scrapper.chat.delete.failed", e);
        }
    }

    @Override
    public LinkResponse addLink(long chatId, String url, List<String> tags, List<String> filters) {
        try {
            var request = new AddLinkRequest(url, tags, filters);
            return scrapperRestClient
                    .post()
                    .uri("/links")
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(LinkResponse.class);
        } catch (RestClientException e) {
            log.atError().addKeyValue("url", url).addKeyValue("chatId", chatId).log("scrapper.link.add.failed", e);
            return null;
        }
    }

    @Override
    public void removeLink(long chatId, String url) {
        try {
            var request = new RemoveLinkRequest(url);
            scrapperRestClient
                    .method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/links")
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.atError().addKeyValue("url", url).addKeyValue("chatId", chatId).log("scrapper.link.remove.failed", e);
        }
    }

    @Override
    public ListLinksResponse getLinks(long chatId) {
        try {
            return scrapperRestClient
                    .get()
                    .uri("/links")
                    .header("Tg-Chat-Id", String.valueOf(chatId))
                    .retrieve()
                    .body(ListLinksResponse.class);
        } catch (RestClientException e) {
            log.atError().addKeyValue("chatId", chatId).log("scrapper.links.fetch.failed", e);
            return null;
        }
    }
}
