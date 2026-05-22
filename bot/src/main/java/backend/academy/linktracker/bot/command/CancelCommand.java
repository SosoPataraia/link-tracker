package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.BotUpdate;
import backend.academy.linktracker.bot.handler.TelegramBotAdapter;
import backend.academy.linktracker.bot.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements BotCommand {

    private final TelegramBotAdapter botAdapter;
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
        botAdapter.sendMessage(update.getChatId(), "Операция отменена.");
    }
}
