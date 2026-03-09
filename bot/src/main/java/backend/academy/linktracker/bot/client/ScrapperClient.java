package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapperClient {

    private final RestClient scrapperRestClient;

    public void registerChat(long chatId) {
        scrapperRestClient.post().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
    }

    public void deleteChat(long chatId) {
        scrapperRestClient.delete().uri("/tg-chat/{id}", chatId).retrieve().toBodilessEntity();
    }

    public LinkResponse addLink(long chatId, String url, java.util.List<String> tags) {
        var request = new AddLinkRequest(url, tags);
        return scrapperRestClient
                .post()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(LinkResponse.class);
    }

    public void removeLink(long chatId, String url) {
        var request = new RemoveLinkRequest(url);
        scrapperRestClient
                .method(org.springframework.http.HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public ListLinksResponse getLinks(long chatId) {
        return scrapperRestClient
                .get()
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(chatId))
                .retrieve()
                .body(ListLinksResponse.class);
    }
}
