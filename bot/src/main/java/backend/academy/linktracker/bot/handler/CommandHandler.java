package backend.academy.linktracker.bot.handler;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class CommandHandler {

    public SendMessage handleCommand(Update update) {
        String messageText = update.message().text();
        long chatId = update.message().chat().id();

        if ("/start".equals(messageText)) {
            return new SendMessage(chatId, "Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды.");
        } else if ("/help".equals(messageText)) {
            return new SendMessage(
                    chatId, "Доступные команды:\n/start - начать работу\n/help - показать список команд");
        } else {
            return new SendMessage(
                    chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.");
        }
    }
}
