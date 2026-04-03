package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.BotUpdate;

public interface BotCommand {
    String command();

    String description();

    void handle(BotUpdate update);
}
