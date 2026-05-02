package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkApiService;
import jakarta.validation.Valid;
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

    private final LinkApiService linkApiService;
    private final LinkRepository linkRepository;

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(@RequestHeader("Tg-Chat-Id") long chatId) {
        return linkApiService
                .getLinks(chatId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody AddLinkRequest request) {

        if (linkRepository.findByChatAndUrl(chatId, request.getLink()).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        return linkApiService
                .addLink(chatId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> removeLink(
            @RequestHeader("Tg-Chat-Id") long chatId, @Valid @RequestBody RemoveLinkRequest request) {

        return linkApiService
                .removeLink(chatId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }
}
