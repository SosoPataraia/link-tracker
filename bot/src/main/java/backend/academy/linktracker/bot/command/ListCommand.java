package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.repository.InMemoryLinkRepository;
import backend.academy.linktracker.bot.repository.SessionRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommand implements BotCommand {

    private final TelegramBot telegramBot;
    private final InMemoryLinkRepository linkRepository;
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
    public void handle(Update update) {
        long chatId = update.message().chat().id();
        String text = update.message().text();
        sessionRepository.getOrCreate(chatId).reset();

        String[] parts = text.trim().split("\\s+", 2);
        String filterTag = parts.length >= 2 ? parts[1].trim() : null;

        List<TrackedLink> links;
        String header;
        if (filterTag != null && !filterTag.isBlank()) {
            links = linkRepository.findByChatAndTag(chatId, filterTag);
            header = "📋 Ссылки с тегом \"" + filterTag + "\":";
        } else {
            links = linkRepository.findAllByChat(chatId);
            header = "📋 Отслеживаемые ссылки:";
        }

        if (links.isEmpty()) {
            String emptyMsg = filterTag != null
                ? "Нет ссылок с тегом \"" + filterTag + "\"."
                : "Вы не отслеживаете ни одной ссылки.\nДобавьте ссылку командой /track";
            telegramBot.execute(new SendMessage(chatId, emptyMsg));
            return;
        }

        var sb = new StringBuilder(header).append("\n\n");
        for (int i = 0; i < links.size(); i++) {
            TrackedLink link = links.get(i);
            sb.append(i + 1).append(". ").append(link.getUrl());
            if (!link.getTags().isEmpty()) {
                sb.append("\n   🏷 Теги: ").append(String.join(", ", link.getTags()));
            }
            sb.append("\n");
        }
        telegramBot.execute(new SendMessage(chatId, sb.toString()));
    }
}
