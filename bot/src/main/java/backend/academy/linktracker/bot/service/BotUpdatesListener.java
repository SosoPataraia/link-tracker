package backend.academy.linktracker.bot.service;

<<<<<<< HEAD
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
=======
import backend.academy.linktracker.bot.handler.CommandHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import java.util.List;
>>>>>>> ef8bc00b69412a3edec9dd7a781f931798749120
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

<<<<<<< HEAD
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
=======
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
>>>>>>> ef8bc00b69412a3edec9dd7a781f931798749120
    }
}
