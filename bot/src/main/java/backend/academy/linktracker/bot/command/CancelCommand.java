package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.BotUpdate;
import backend.academy.linktracker.bot.repository.SessionRepository;
import com.pengrad.telegrambot.TelegramBot;
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
    public void handle(BotUpdate update) {
        sessionRepository.getOrCreate(update.getChatId()).reset();
        telegramBot.execute(new SendMessage(update.getChatId(), "Операция отменена."));
    }
}
