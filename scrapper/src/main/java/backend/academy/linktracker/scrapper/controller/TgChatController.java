package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final LinkRepository linkRepository;

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/{id}")
    public void registerChat(@PathVariable long id) {
        log.atInfo().addKeyValue("chatId", id).log("chat.register");
        chatRepository.register(id);
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/{id}")
    public void deleteChat(@PathVariable long id) {
        if (!chatRepository.exists(id)) {
            throw new ChatNotFoundException(id);
        }
        log.atInfo().addKeyValue("chatId", id).log("chat.delete");
        linkRepository.removeAllByChat(id);
        chatRepository.remove(id);
    }
}
