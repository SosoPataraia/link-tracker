package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.handler.CommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotUpdatesListener implements UpdatesListener {

    private final TelegramBot telegramBot;
    private final CommandHandler commandHandler;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
        log.info("Bot updates listener started");
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            processUpdate(update);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            log.debug("Skipping update without message", "updateId", update.updateId());
            return;
        }

        long chatId = update.message().chat().id();
        String text = update.message().text();

        log.info("Received message",
            "chatId", chatId,
            "updateId", update.updateId(),
            "text", text);

        var response = commandHandler.handleCommand(update);
        telegramBot.execute(response);

        log.info("Sent response",
            "chatId", chatId,
            "command", text);
    }
}
