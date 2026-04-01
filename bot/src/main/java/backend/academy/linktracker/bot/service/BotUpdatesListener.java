package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.handler.CommandHandler;
import backend.academy.linktracker.bot.properties.TelegramProperties;
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
    private final TelegramProperties properties;

    @PostConstruct
    public void init() {
        if (properties.isAutoStartListener()) {
            telegramBot.setUpdatesListener(this);
            log.info("Bot updates listener started");
        } else {
            log.info("Bot updates listener auto-start disabled");
        }
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            processUpdate(update);
        }
        return CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            log.debug("Skipping update without message");
            return;
        }

        long chatId = update.message().chat().id();
        String text = update.message().text();

        log.info("Received message chatId={} updateId={} text={}", chatId, update.updateId(), text);

        var response = commandHandler.handleCommand(update);
        telegramBot.execute(response);

        log.info("Sent response chatId={} command={}", chatId, text);
    }
}
