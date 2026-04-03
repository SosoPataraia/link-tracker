package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.BotUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements BotCommand {

    private final TelegramBot telegramBot;
    private final List<BotCommand> commands;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Справка по командам";
    }

    @Override
    public void handle(BotUpdate update) {
        String commandList = commands.stream()
                .map(cmd -> cmd.command() + " — " + cmd.description())
                .collect(Collectors.joining("\n"));
        String text = "ℹ️ Справка по командам:\n\n" + commandList
                + "\n\nПоддерживаемые ресурсы:\n"
                + "• GitHub репозитории (github.com/...)\n"
                + "• StackOverflow вопросы (stackoverflow.com/questions/...)";
        telegramBot.execute(new SendMessage(update.getChatId(), text));
    }
}
