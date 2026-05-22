package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.BotUpdate;
import backend.academy.linktracker.bot.handler.TelegramBotAdapter;
import backend.academy.linktracker.bot.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UntrackCommand implements BotCommand {

    private final TelegramBotAdapter botAdapter;
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
    public void handle(BotUpdate update) {
        long chatId = update.getChatId();
        String text = update.getText();
        sessionRepository.getOrCreate(chatId).reset();

        String[] parts = text.trim().split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            botAdapter.sendMessage(chatId, "Укажите ссылку: /untrack <ссылка>");
            return;
        }
        String url = parts[1].trim();

        try {
            scrapperClient.removeLink(chatId, url);
            log.atInfo().addKeyValue("url", url).addKeyValue("chatId", chatId).log("link.untracked");
            botAdapter.sendMessage(chatId, "✅ Отслеживание ссылки прекращено:\n" + url);
        } catch (Exception e) {
            log.atWarn()
                    .addKeyValue("url", url)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("error", e.getMessage())
                    .log("link.untrack.failed");
            botAdapter.sendMessage(chatId, "❌ Ссылка не найдена в списке отслеживаемых.");
        }
    }
}
