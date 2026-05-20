package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateNotificationHandlerImpl implements UpdateNotificationHandler {

    private final TelegramBotAdapter telegramBotAdapter;

    @Override
    public void handleUpdate(LinkUpdate update) {
        String message = buildMessage(update);
        for (Long chatId : update.getTgChatIds()) {
            log.atInfo()
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", update.getUrl())
                    .log("update.notification.sent");
            telegramBotAdapter.sendMessage(chatId, message);
        }
    }

    private String buildMessage(LinkUpdate update) {
        String desc = update.getDescription() != null ? update.getDescription() : "Обнаружены изменения";
        return "\uD83D\uDD14 Обновление по ссылке:\n" + update.getUrl() + "\n\n" + desc;
    }
}
