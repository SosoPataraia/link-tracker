package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateNotificationHandler {

    private final TelegramBot telegramBot;

    public void handleUpdate(LinkUpdate update) {
        String message = buildMessage(update);
        for (Long chatId : update.getTgChatIds()) {
            log.info("Sending update notification to chatId={} url={}", chatId, update.getUrl());
            telegramBot.execute(new SendMessage(chatId, message));
        }
    }

    private String buildMessage(LinkUpdate update) {
        String desc = update.getDescription() != null ? update.getDescription() : "Обнаружены изменения";
        return "\uD83D\uDD14 Обновление по ссылке:\n" + update.getUrl() + "\n\n" + desc;
    }
}
