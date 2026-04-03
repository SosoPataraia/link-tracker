package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.BotUpdate;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.repository.SessionRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommand implements BotCommand {

    private final TelegramBot telegramBot;
    private final ScrapperClient scrapperClient;
    private final SessionRepository sessionRepository;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Список отслеживаемых ссылок";
    }

    @Override
    public void handle(BotUpdate update) {
        long chatId = update.getChatId();
        String text = update.getText();
        sessionRepository.getOrCreate(chatId).reset();

        String[] parts = text.trim().split("\\s+", 2);
        String filterTag = parts.length >= 2 ? parts[1].trim() : null;

        var response = scrapperClient.getLinks(chatId);
        List<LinkResponse> links = (response != null && response.getLinks() != null) ? response.getLinks() : List.of();

        if (filterTag != null && !filterTag.isBlank()) {
            String tag = filterTag;
            links = links.stream()
                    .filter(l -> l.getTags() != null && l.getTags().contains(tag))
                    .toList();
        }

        if (links.isEmpty()) {
            String emptyMsg = filterTag != null
                    ? "Нет ссылок с тегом \"" + filterTag + "\"."
                    : "Вы не отслеживаете ни одной ссылки.\nДобавьте ссылку командой /track";
            telegramBot.execute(new SendMessage(chatId, emptyMsg));
            return;
        }

        String header = filterTag != null ? "📋 Ссылки с тегом \"" + filterTag + "\":" : "📋 Отслеживаемые ссылки:";

        var sb = new StringBuilder(header).append("\n\n");
        for (int i = 0; i < links.size(); i++) {
            LinkResponse link = links.get(i);
            sb.append(i + 1).append(". ").append(link.getUrl());
            if (link.getTags() != null && !link.getTags().isEmpty()) {
                sb.append("\n   🏷 Теги: ").append(String.join(", ", link.getTags()));
            }
            sb.append("\n");
        }
        telegramBot.execute(new SendMessage(chatId, sb.toString()));
    }
}
