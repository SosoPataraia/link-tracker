package backend.academy.linktracker.bot.service;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotUpdatesListener {

    private final TelegramBot telegramBot;
    private final TelegramUpdateHandler telegramUpdateHandler; // Inject the handler
    private final Gson gson; // Inject Gson if needed, or create locally

    @PostConstruct
    public void init() {
        log.info("Initializing Updates Listener...");
        // Set the updates listener on the bot instance
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                try {
                    log.debug("Received update: {}", gson.toJson(update)); // Log the raw update for debugging
                    // Delegate the processing to the handler
                    telegramUpdateHandler.handleUpdate(update);
                } catch (Exception e) {
                    log.error("Error processing update: {}", update, e);
                    // Optionally, send an error message back to the user
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL; // Confirm all updates were processed
        });
        log.info("Updates Listener initialized.");
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down Updates Listener...");
        // Remove the listener when the application shuts down
        telegramBot.removeGetUpdatesListener();
    }
}
