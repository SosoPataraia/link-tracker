package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;

public interface LinkService {

    ListLinksResponse getLinks(long chatId);

    LinkResponse addLink(long chatId, AddLinkRequest request);

    LinkResponse removeLink(long chatId, RemoveLinkRequest request);

    /**
     * Walks all tracked links in keyset batches, fetches updates, notifies subscribers in parallel,
     * and sends a notice for links that could not be processed. Advances last-checked only for links
     * handled without error.
     */
    void checkAllLinks();
}
