package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinksController {

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(@RequestHeader("Tg-Chat-Id") long chatId) {
        if (!chatRepository.exists(chatId)) {
            return ResponseEntity.status(400).body(new ListLinksResponse(List.of(), 0));
        }
        List<TrackedLink> links = linkRepository.findAllByChat(chatId);
        List<LinkResponse> responses = links.stream()
                .map(l -> new LinkResponse(l.getId(), l.getUrl(), l.getTags()))
                .toList();
        return ResponseEntity.ok(new ListLinksResponse(responses, responses.size()));
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody AddLinkRequest request) {

        if (!chatRepository.exists(chatId)) {
            return ResponseEntity.status(400).build();
        }
        if (linkRepository.findByChatAndUrl(chatId, request.getLink()).isPresent()) {
            return ResponseEntity.status(409).build();
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
        return ResponseEntity.ok(new LinkResponse(saved.getId(), saved.getUrl(), saved.getTags()));
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> removeLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody RemoveLinkRequest request) {

        if (!chatRepository.exists(chatId)) {
            return ResponseEntity.status(404).build();
        }
        var existing = linkRepository.findByChatAndUrl(chatId, request.getLink());
        if (existing.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        linkRepository.remove(chatId, request.getLink());
        log.atInfo()
                .addKeyValue("url", request.getLink())
                .addKeyValue("chatId", chatId)
                .log("link.removed");
        var removed = existing.orElseThrow();
        return ResponseEntity.ok(new LinkResponse(removed.getId(), removed.getUrl(), removed.getTags()));
    }
}
