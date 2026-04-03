package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.repository.SessionRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UntrackCommand implements BotCommand {

    private final TelegramBot telegramBot;
    private final ScrapperClient scrapperClient;
    private final SessionRepository sessionRepository;

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Прекратить отслеживание ссылки";
    }

    @Override
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        String text = update.message().text();
        sessionRepository.getOrCreate(chatId).reset();

        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            telegramBot.execute(new SendMessage(chatId, "Укажите ссылку: /untrack <ссылка>"));
            return;
        }
        String url = parts[1].trim();

        try {
            scrapperClient.removeLink(chatId, url);
            log.atInfo().addKeyValue("url", url).addKeyValue("chatId", chatId).log("link.untracked");
            telegramBot.execute(new SendMessage(chatId, "✅ Отслеживание ссылки прекращено:\n" + url));
        } catch (Exception e) {
            log.warn("Failed to remove link chatId={} url={}: {}", chatId, url, e.getMessage());
            telegramBot.execute(new SendMessage(chatId, "❌ Ссылка не найдена в списке отслеживаемых."));
        }
    }
}
