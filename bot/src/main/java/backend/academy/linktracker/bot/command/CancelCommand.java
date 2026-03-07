package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.repository.SessionRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements BotCommand {

    private final TelegramBot telegramBot;
    private final SessionRepository sessionRepository;

    @Override
    public String command() {
        return "/cancel";
    }

    @Override
    public String description() {
        return "Отменить текущую операцию";
    }

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        sessionRepository.getOrCreate(chatId).reset();
        telegramBot.execute(new SendMessage(chatId, "Операция отменена."));
    }
}
