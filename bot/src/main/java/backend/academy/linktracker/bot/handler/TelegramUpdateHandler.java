package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.BotCommand;
import backend.academy.linktracker.bot.model.TrackedLink;
import backend.academy.linktracker.bot.model.UserSession;
import backend.academy.linktracker.bot.repository.InMemoryLinkRepository;
import backend.academy.linktracker.bot.repository.SessionRepository;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TelegramUpdateHandler {

    private final TelegramBot telegramBot;
    private final SessionRepository sessionRepository;
    private final InMemoryLinkRepository linkRepository;
    private final ScrapperClient scrapperClient;
    private final Map<String, BotCommand> commandMap;

    public TelegramUpdateHandler(
            TelegramBot telegramBot,
            SessionRepository sessionRepository,
            InMemoryLinkRepository linkRepository,
            ScrapperClient scrapperClient,
            List<BotCommand> commands) {
        this.telegramBot = telegramBot;
        this.sessionRepository = sessionRepository;
        this.linkRepository = linkRepository;
        this.scrapperClient = scrapperClient;
        this.commandMap = commands.stream().collect(Collectors.toMap(BotCommand::command, Function.identity()));
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                try {
                    handleUpdate(update);
                } catch (Exception e) {
                    log.error("Error handling update={}", update.updateId(), e);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        log.info("Bot started, registered {} commands", commandMap.size());
    }

    public void processUpdate(Update update) {
        try {
            handleUpdate(update);
        } catch (Exception e) {
            log.error("Error handling update", e);
        }
    }

    private void handleUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }

        long chatId = update.message().chat().id();
        String text = update.message().text().trim();
        UserSession session = sessionRepository.getOrCreate(chatId);

        log.info(
                "Handling update chatId={} state={} text={}",
                chatId,
                session.getState(),
                text.length() > 50 ? text.substring(0, 50) + "..." : text);

        if (text.startsWith("/")) {
            String commandKey = text.split("\\s+")[0].toLowerCase();
            BotCommand cmd = commandMap.get(commandKey);
            if (cmd != null) {
                if (!commandKey.equals("/track") && !commandKey.equals("/cancel")) {
                    session.reset();
                }
                cmd.handle(update);
                return;
            }
            // Unknown command
            if (session.getState() != UserState.IDLE) {
                session.reset();
            }
            telegramBot.execute(
                    new SendMessage(chatId, "Неизвестная команда: " + commandKey + "\nВведите /help для справки."));
            return;
        }

        switch (session.getState()) {
            case WAITING_FOR_LINK -> handleLinkInput(chatId, text, session);
            case WAITING_FOR_TAGS -> handleTagsInput(chatId, text, session);
            default -> telegramBot.execute(new SendMessage(chatId, "Введите команду. Используйте /help для справки."));
        }
    }

    private void handleLinkInput(long chatId, String url, UserSession session) {
        if (!isValidUrl(url)) {
            telegramBot.execute(
                    new SendMessage(
                            chatId,
                            "❌ Некорректная ссылка. Пожалуйста, введите корректный URL (например, https://github.com/user/repo)"));
            return;
        }

        if (linkRepository.exists(chatId, url)) {
            session.reset();
            telegramBot.execute(new SendMessage(chatId, "Ссылка уже отслеживается"));
            return;
        }

        session.setPendingUrl(url);
        session.setState(UserState.WAITING_FOR_TAGS);
        telegramBot.execute(new SendMessage(
                chatId,
                "✅ Ссылка принята: " + url + "\n\n"
                        + "Введите теги через запятую (необязательно).\n"
                        + "Например: работа, баг, документация\n\n"
                        + "Или отправьте пустое сообщение / /skip чтобы пропустить."));
    }

    private void handleTagsInput(long chatId, String input, UserSession session) {
        String url = session.getPendingUrl();
        List<String> tags = List.of();

        if (!input.isBlank() && !input.equalsIgnoreCase("/skip")) {
            tags = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isBlank())
                    .toList();
        }

        var link = new TrackedLink(url, new java.util.ArrayList<>(tags));
        linkRepository.save(chatId, link);

        try {
            scrapperClient.registerChat(chatId);
            scrapperClient.addLink(chatId, url, tags);
            log.info("Registered link url={} tags={} for chatId={}", url, tags, chatId);
        } catch (Exception e) {
            log.warn("Failed to register link in scrapper for chatId={} url={}: {}", chatId, url, e.getMessage());
        }

        session.reset();

        String tagsInfo = tags.isEmpty() ? "" : "\n🏷 Теги: " + String.join(", ", tags);
        telegramBot.execute(new SendMessage(chatId, "✅ Ссылка добавлена в отслеживание:\n" + url + tagsInfo));
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.equals("http") || scheme.equals("https"))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}
