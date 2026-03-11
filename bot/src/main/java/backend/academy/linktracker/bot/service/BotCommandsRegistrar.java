package backend.academy.linktracker.bot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotCommandsRegistrar {

    private final TelegramBot telegramBot;

    @PostConstruct
    public void registerCommands() {
        var commands = new BotCommand[] {
            new BotCommand("start", "Начать работу с ботом"),
            new BotCommand("help", "Показать список доступных команд")
        };

        var request = new SetMyCommands(commands);
        var response = telegramBot.execute(request);

        if (response.isOk()) {
            log.info("Bot commands registered successfully",
                "commandCount", commands.length);
        } else {
            log.error("Failed to register bot commands",
                "errorCode", response.errorCode(),
                "description", response.description());
        }
    }
}
