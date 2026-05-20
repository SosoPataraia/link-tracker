package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;

    public ListLinksResponse getLinks(long chatId, int limit, int offset) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        List<TrackedLink> links = linkRepository.findAllByChat(chatId, limit, offset);
        List<LinkResponse> responses = links.stream()
                .map(l -> new LinkResponse(l.getId(), l.getUrl(), l.getTags()))
                .toList();
        return new ListLinksResponse(responses, responses.size());
    }

    @Transactional
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        if (linkRepository.findByChatAndUrl(chatId, request.getLink()).isPresent()) {
            throw new LinkAlreadyTrackedException(request.getLink());
        }
        var link = new TrackedLink(
                null,
                chatId,
                request.getLink(),
                new ArrayList<>(request.getTags() != null ? request.getTags() : List.of()),
                Instant.now(),
                Instant.now());
        TrackedLink saved = linkRepository.save(link);
        log.atInfo()
                .addKeyValue("url", request.getLink())
                .addKeyValue("chatId", chatId)
                .log("link.added");
        return new LinkResponse(saved.getId(), saved.getUrl(), saved.getTags());
    }

    @Transactional
    public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        var existing = linkRepository
                .findByChatAndUrl(chatId, request.getLink())
                .orElseThrow(() -> new LinkNotFoundException(request.getLink()));
        linkRepository.remove(chatId, request.getLink());
        log.atInfo()
                .addKeyValue("url", request.getLink())
                .addKeyValue("chatId", chatId)
                .log("link.removed");
        return new LinkResponse(existing.getId(), existing.getUrl(), existing.getTags());
    }
}
