package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.Command;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotCommandsRegistrar {

    private final TelegramBot telegramBot;
    private final List<Command> commands;

    @PostConstruct
    public void registerCommands() {
        try {
            BotCommand[] botCommands = commands.stream()
                    .map(c -> new BotCommand(c.command().replace("/", ""), c.description()))
                    .toArray(BotCommand[]::new);

            var response = telegramBot.execute(new SetMyCommands(botCommands));

            if (response.isOk()) {
                log.info("Bot commands registered successfully");
            } else {
                log.error(
                        "Failed to register bot commands: errorCode={} description={}",
                        response.errorCode(),
                        response.description());
            }
        } catch (Exception e) {
            log.error("Failed to register bot commands", e);
        }
    }
}
