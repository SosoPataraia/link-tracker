package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class UnknownCommand implements Command {

    @Override
    public String command() {
        return "unknown";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        return new SendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.");
    }
}
