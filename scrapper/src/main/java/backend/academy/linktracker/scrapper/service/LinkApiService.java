package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import java.util.Optional;

public interface LinkApiService {

    Optional<ListLinksResponse> getLinks(long chatId);

    Optional<LinkResponse> addLink(long chatId, AddLinkRequest request);

    Optional<LinkResponse> removeLink(long chatId, RemoveLinkRequest request);
}
