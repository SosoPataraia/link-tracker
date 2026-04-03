package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.BotUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements BotCommand {

    private final TelegramBot telegramBot;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Начать работу с ботом";
    }

    @Override
    public void handle(BotUpdate update) {
        String text = "Привет, " + update.getFirstName() + "! \uD83D\uDC4B\n\n"
                + "Я бот для отслеживания изменений на веб-страницах.\n\n"
                + "Доступные команды:\n"
                + "/track — начать отслеживание ссылки\n"
                + "/untrack — прекратить отслеживание ссылки\n"
                + "/list — список отслеживаемых ссылок\n"
                + "/help — справка";
        telegramBot.execute(new SendMessage(update.getChatId(), text));
    }
}
