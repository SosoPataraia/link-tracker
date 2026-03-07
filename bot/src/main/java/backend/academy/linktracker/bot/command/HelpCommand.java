package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements BotCommand {

    private final TelegramBot telegramBot;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Справка по командам";
    }

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        String text = "ℹ️ Справка по командам:\n\n"
            + "/start — начать работу с ботом\n"
            + "/track — начать отслеживание ссылки (поддерживает теги)\n"
            + "/untrack — прекратить отслеживание ссылки\n"
            + "/list — показать все отслеживаемые ссылки\n"
            + "/list <тег> — показать ссылки с указанным тегом\n"
            + "/help — эта справка\n\n"
            + "Поддерживаемые ресурсы:\n"
            + "• GitHub репозитории (github.com/...)\n"
            + "• StackOverflow вопросы (stackoverflow.com/questions/...)";
        telegramBot.execute(new SendMessage(chatId, text));
    }
}
