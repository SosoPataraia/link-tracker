package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.InMemoryLinkRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    private final InMemoryLinkRepository linkRepository;
    private final ChatRepository chatRepository;

    @GetMapping
    public ResponseEntity<?> getLinks(@RequestHeader("Tg-Chat-Id") long chatId) {
        if (!chatRepository.exists(chatId)) {
            return ResponseEntity.status(400)
                    .body(new ApiErrorResponse(
                            "Chat not registered",
                            "400",
                            "ChatNotFoundException",
                            "Chat " + chatId + " not found",
                            List.of()));
        }
        List<TrackedLink> links = linkRepository.findAllByChat(chatId);
        List<LinkResponse> responses = links.stream()
                .map(l -> new LinkResponse(l.getId(), l.getUrl(), l.getTags()))
                .toList();
        return ResponseEntity.ok(new ListLinksResponse(responses, responses.size()));
    }

    @PostMapping
    public ResponseEntity<?> addLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody AddLinkRequest request) {

        if (!chatRepository.exists(chatId)) {
            return ResponseEntity.status(400)
                    .body(new ApiErrorResponse(
                            "Chat not registered",
                            "400",
                            "ChatNotFoundException",
                            "Chat " + chatId + " not found",
                            List.of()));
        }

        if (linkRepository.findByChatAndUrl(chatId, request.getLink()).isPresent()) {
            return ResponseEntity.status(409)
                    .body(new ApiErrorResponse(
                            "Link already tracked",
                            "409",
                            "LinkAlreadyExistsException",
                            "Link already tracked: " + request.getLink(),
                            List.of()));
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
        return ResponseEntity.ok(new LinkResponse(saved.getId(), saved.getUrl(), saved.getTags()));
    }

    @DeleteMapping
    public ResponseEntity<?> removeLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody RemoveLinkRequest request) {

        if (!chatRepository.exists(chatId)) {
            return ResponseEntity.status(404)
                    .body(new ApiErrorResponse(
                            "Chat not found",
                            "404",
                            "ChatNotFoundException",
                            "Chat " + chatId + " not found",
                            List.of()));
        }

        var existing = linkRepository.findByChatAndUrl(chatId, request.getLink());
        if (existing.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(new ApiErrorResponse(
                            "Link not found",
                            "404",
                            "LinkNotFoundException",
                            "Link not found: " + request.getLink(),
                            List.of()));
        }

        linkRepository.remove(chatId, request.getLink());
        log.info("Removed link url={} for chatId={}", request.getLink(), chatId);
        var removed = existing.orElseThrow();
        return ResponseEntity.ok(new LinkResponse(removed.getId(), removed.getUrl(), removed.getTags()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        "Invalid request",
                        "400",
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        Arrays.stream(ex.getStackTrace())
                                .map(StackTraceElement::toString)
                                .limit(5)
                                .toList()));
    }
}
