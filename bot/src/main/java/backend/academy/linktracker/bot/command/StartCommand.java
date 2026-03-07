package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
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
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        String firstName = update.message().from().firstName();
        String text = "Привет, " + firstName + "! \uD83D\uDC4B\n\n"
            + "Я бот для отслеживания изменений на веб-страницах.\n\n"
            + "Доступные команды:\n"
            + "/track — начать отслеживание ссылки\n"
            + "/untrack — прекратить отслеживание ссылки\n"
            + "/list — список отслеживаемых ссылок\n"
            + "/help — справка";
        telegramBot.execute(new SendMessage(chatId, text));
    }
}
