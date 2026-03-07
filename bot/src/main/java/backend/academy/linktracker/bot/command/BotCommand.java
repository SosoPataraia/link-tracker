package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;

public interface BotCommand {

    String command();

    String description();

    void handle(Update update);
}
