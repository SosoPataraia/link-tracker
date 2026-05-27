package backend.academy.linktracker.bot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotAdapterImpl implements TelegramBotAdapter {

    private final TelegramBot telegramBot;

    @Override
    public void sendMessage(long chatId, String text) {
        try {
            telegramBot.execute(new SendMessage(chatId, text));
        } catch (Exception e) {
            log.atError().addKeyValue("chatId", chatId).log("telegram.send.failed", e);
        }
    }

    @Override
    public void setUpdatesListener(UpdatesListener listener) {
        telegramBot.setUpdatesListener(listener);
    }
}
