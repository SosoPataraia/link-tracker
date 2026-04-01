package backend.academy.linktracker.bot.handler;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommandHandler {

    public SendMessage handleCommand(Update update) {
        String text = update.message().text();
        long chatId = update.message().chat().id();

        log.info("Handling command", "chatId", chatId, "command", text);

        return switch (text) {
            case "/start" -> {
                log.info("Processing /start", "chatId", chatId);
                yield new SendMessage(
                        chatId, "Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды.");
            }
            case "/help" -> {
                log.info("Processing /help", "chatId", chatId);
                yield new SendMessage(
                        chatId, "Доступные команды:\n/start — начать работу\n/help — показать список команд");
            }
            default -> {
                log.warn("Unknown command", "chatId", chatId, "command", text);
                yield new SendMessage(
                        chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.");
            }
        };
    }
}
