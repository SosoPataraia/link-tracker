package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.BotUpdate;
import backend.academy.linktracker.bot.handler.TelegramBotAdapter;
import backend.academy.linktracker.bot.repository.SessionRepository;
import backend.academy.linktracker.bot.state.UserState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackCommand implements BotCommand {

    private final TelegramBotAdapter telegramBotAdapter;
    private final SessionRepository sessionRepository;

    @Override
    public String command() {
        return "/track";
    }

    @Override
    public String description() {
        return "Начать отслеживание ссылки";
    }

    @Override
    public void handle(BotUpdate update) {
        var session = sessionRepository.getOrCreate(update.getChatId());
        session.setState(UserState.WAITING_FOR_LINK);
        session.setPendingUrl(null);
        telegramBotAdapter.sendMessage(
                update.getChatId(),
                "Отправьте ссылку для отслеживания.\n"
                        + "Поддерживаются: github.com и stackoverflow.com\n\n"
                        + "Для отмены введите /cancel");
    }
}
