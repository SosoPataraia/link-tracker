package backend.academy.linktracker.bot.handler;

public interface TelegramBotAdapter {
    void sendMessage(long chatId, String text);
}
