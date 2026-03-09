package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.repository.InMemoryLinkRepository;
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
    private final InMemoryLinkRepository linkRepository;
    private final SessionRepository sessionRepository;
    private final ScrapperClient scrapperClient;

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

        // Extract URL from command: /untrack <url>
        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            telegramBot.execute(new SendMessage(chatId, "Укажите ссылку: /untrack <ссылка>"));
            return;
        }
        String url = parts[1].trim();

        if (!linkRepository.exists(chatId, url)) {
            telegramBot.execute(new SendMessage(chatId, "Ссылка не найдена в списке отслеживаемых."));
            return;
        }

        try {
            scrapperClient.removeLink(chatId, url);
        } catch (Exception e) {
            log.warn("Failed to remove link from scrapper for chatId={} url={}: {}", chatId, url, e.getMessage());
        }
        linkRepository.remove(chatId, url);
        log.info("Untracked link url={} for chatId={}", url, chatId);
        telegramBot.execute(new SendMessage(chatId, "✅ Отслеживание ссылки прекращено:\n" + url));
    }
}
