package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import java.util.List;

public interface ScrapperClient {
    void registerChat(long chatId);

    void deleteChat(long chatId);

    LinkResponse addLink(long chatId, String url, List<String> tags, List<String> filters);

    void removeLink(long chatId, String url);

    ListLinksResponse getLinks(long chatId);
}
