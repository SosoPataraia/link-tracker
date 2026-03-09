package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.repository.SessionRepository;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackCommand implements BotCommand {

    private final TelegramBot telegramBot;
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
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        var session = sessionRepository.getOrCreate(chatId);
        session.setState(UserState.WAITING_FOR_LINK);
        session.setPendingUrl(null);
        telegramBot.execute(new SendMessage(
                chatId,
                "Отправьте ссылку для отслеживания.\n"
                        + "Поддерживаются: github.com и stackoverflow.com\n\n"
                        + "Для отмены введите /cancel"));
    }
}
