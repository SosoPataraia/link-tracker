package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    @Override
    @Transactional
    public void registerChat(long chatId) {
        chatRepository.register(chatId);
        log.atInfo().addKeyValue("chatId", chatId).log("chat.registered");
    }

    @Override
    @Transactional
    public void deleteChat(long chatId) {
        if (!chatRepository.exists(chatId)) {
            log.atWarn().addKeyValue("chatId", chatId).log("chat.not.found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        }
        linkRepository.removeAllByChat(chatId);
        chatRepository.remove(chatId);
        log.atInfo().addKeyValue("chatId", chatId).log("chat.deleted");
    }
}
