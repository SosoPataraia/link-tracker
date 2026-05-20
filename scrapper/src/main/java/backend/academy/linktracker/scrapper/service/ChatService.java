package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    public void register(long chatId) {
        log.atInfo().addKeyValue("chatId", chatId).log("chat.register");
        chatRepository.register(chatId);
    }

    @Transactional
    public void delete(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        log.atInfo().addKeyValue("chatId", chatId).log("chat.delete");
        linkRepository.removeAllByChat(chatId);
        chatRepository.remove(chatId);
    }
}
