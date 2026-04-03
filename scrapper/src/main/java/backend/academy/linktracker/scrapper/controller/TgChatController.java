package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.InMemoryLinkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
public class TgChatController {

    private final ChatRepository chatRepository;
    private final InMemoryLinkRepository linkRepository;

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{id}")
    public void registerChat(@PathVariable long id) {
        log.info("Registering chat chatId={}", id);
        chatRepository.register(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiErrorResponse> deleteChat(@PathVariable long id) {
        if (!chatRepository.exists(id)) {
            log.warn("Chat not found chatId={}", id);
            return ResponseEntity.status(404)
                    .body(new ApiErrorResponse(
                            "Chat not found",
                            "404",
                            "ChatNotFoundException",
                            "Chat with id " + id + " not found",
                            List.of()));
        }
        log.info("Deleting chat chatId={}", id);
        linkRepository.removeAllByChat(id);
        chatRepository.remove(id);
        return ResponseEntity.ok().build();
    }
}
