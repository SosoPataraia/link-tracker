package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.UnknownCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandHandler {

    private final Map<String, Command> commands;
    private final UnknownCommand unknownCommand;

    public CommandHandler(List<Command> commandList, UnknownCommand unknownCommand) {
        this.unknownCommand = unknownCommand;
        this.commands = commandList.stream().collect(Collectors.toMap(Command::command, Function.identity()));
    }

    public SendMessage handleCommand(Update update) {
        String text = update.message().text();
        long chatId = update.message().chat().id();

        log.info("Handling command chatId={} command={}", chatId, text);

        Command command = commands.getOrDefault(text, unknownCommand);
        return command.handle(update);
    }
}
