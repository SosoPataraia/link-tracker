package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {

    private final List<Command> commands;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Показать список доступных команд";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        String text = commands.stream()
                .map(c -> c.command() + " — " + c.description())
                .collect(Collectors.joining("\n"));
        return new SendMessage(chatId, "Доступные команды:\n" + text);
    }
}
