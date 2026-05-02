package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.cache.LocalLinksCache;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkApiService {

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;

    @Autowired(required = false)
    private LocalLinksCache localLinksCache;

    @Cacheable(value = "links", key = "#chatId")
    public Optional<ListLinksResponse> getLinks(long chatId) {
        // L1 check
        if (localLinksCache != null) {
            ListLinksResponse cached = localLinksCache.get(chatId);
            if (cached != null) {
                log.debug("L1 cache hit for chatId={}", chatId);
                return Optional.of(cached);
            }
        }

        if (!chatRepository.exists(chatId)) {
            return Optional.empty();
        }
        List<TrackedLink> links = linkRepository.findAllByChat(chatId);
        List<LinkResponse> responses = links.stream()
                .map(l -> new LinkResponse(l.getId(), l.getUrl(), l.getTags()))
                .toList();
        var result = new ListLinksResponse(responses, responses.size());

        // populate L1
        if (localLinksCache != null) {
            localLinksCache.put(chatId, result);
        }
        return Optional.of(result);
    }

    @CacheEvict(value = "links", key = "#chatId")
    public Optional<LinkResponse> addLink(long chatId, AddLinkRequest request) {
        if (localLinksCache != null) localLinksCache.evict(chatId);

        if (!chatRepository.exists(chatId)) {
            return Optional.empty();
        }
        if (linkRepository.findByChatAndUrl(chatId, request.getLink()).isPresent()) {
            return Optional.empty();
        }
        var link = new TrackedLink(
                null,
                chatId,
                request.getLink(),
                new ArrayList<>(request.getTags() != null ? request.getTags() : List.of()),
                Instant.now(),
                Instant.now());
        TrackedLink saved = linkRepository.save(link);
        log.info("Added link url={} for chatId={}", request.getLink(), chatId);
        return Optional.of(new LinkResponse(saved.getId(), saved.getUrl(), saved.getTags()));
    }

    @CacheEvict(value = "links", key = "#chatId")
    public Optional<LinkResponse> removeLink(long chatId, RemoveLinkRequest request) {
        if (localLinksCache != null) localLinksCache.evict(chatId);

        if (!chatRepository.exists(chatId)) {
            return Optional.empty();
        }
        var existing = linkRepository.findByChatAndUrl(chatId, request.getLink());
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        linkRepository.remove(chatId, request.getLink());
        log.info("Removed link url={} for chatId={}", request.getLink(), chatId);
        var removed = existing.orElseThrow();
        return Optional.of(new LinkResponse(removed.getId(), removed.getUrl(), removed.getTags()));
    }
}
