package backend.academy.linktracker.bot.handler;

import com.pengrad.telegrambot.UpdatesListener;

public interface TelegramBotAdapter {

    void sendMessage(long chatId, String text);

    void setUpdatesListener(UpdatesListener listener);
}
