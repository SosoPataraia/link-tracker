package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.BotCommand;
import backend.academy.linktracker.bot.dto.BotUpdate;
import backend.academy.linktracker.bot.model.UserSession;
import backend.academy.linktracker.bot.repository.SessionRepository;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
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
    private final TelegramBotAdapter telegramBotAdapter;
    private final SessionRepository sessionRepository;
    private final ScrapperClient scrapperClient;
    private final Map<String, BotCommand> commandMap;

    public TelegramUpdateHandler(
            TelegramBot telegramBot,
            TelegramBotAdapter telegramBotAdapter,
            SessionRepository sessionRepository,
            ScrapperClient scrapperClient,
            List<BotCommand> commands) {
        this.telegramBot = telegramBot;
        this.telegramBotAdapter = telegramBotAdapter;
        this.sessionRepository = sessionRepository;
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
                    log.atError().addKeyValue("updateId", update.updateId()).log("update.handling.failed", e);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        log.atInfo().addKeyValue("commandCount", commandMap.size()).log("bot.started");
    }

    public void handleUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }

        long chatId = update.message().chat().id();
        String text = update.message().text().trim();
        String firstName =
                update.message().from() != null ? update.message().from().firstName() : "";
        String username =
                update.message().from() != null ? update.message().from().username() : "";

        BotUpdate botUpdate = new BotUpdate(chatId, text, firstName, username);
        UserSession session = sessionRepository.getOrCreate(chatId);

        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("state", session.getState())
                .log("update.received");

        if (text.startsWith("/")) {
            String commandKey = text.split("\\s+")[0].toLowerCase();

            if (commandKey.equals("/skip")) {
                switch (session.getState()) {
                    case WAITING_FOR_TAGS -> handleTagsInput(chatId, "", session);
                    case WAITING_FOR_FILTERS -> handleFiltersInput(chatId, "", session);
                    default -> telegramBotAdapter.sendMessage(chatId, "Нет активной операции для пропуска.");
                }
                return;
            }

            BotCommand cmd = commandMap.get(commandKey);
            if (cmd != null) {
                if (!commandKey.equals("/track") && !commandKey.equals("/cancel")) {
                    session.reset();
                }
                cmd.handle(botUpdate);
                return;
            }

            if (session.getState() != UserState.IDLE) {
                session.reset();
            }
            telegramBotAdapter.sendMessage(
                    chatId, "Неизвестная команда: " + commandKey + "\nВведите /help для справки.");
            return;
        }

        switch (session.getState()) {
            case WAITING_FOR_LINK -> handleLinkInput(chatId, text, session);
            case WAITING_FOR_TAGS -> handleTagsInput(chatId, text, session);
            case WAITING_FOR_FILTERS -> handleFiltersInput(chatId, text, session);
            default -> telegramBotAdapter.sendMessage(chatId, "Введите команду. Используйте /help для справки.");
        }
    }

    private void handleLinkInput(long chatId, String url, UserSession session) {
        if (!isValidUrl(url)) {
            telegramBotAdapter.sendMessage(
                    chatId,
                    "❌ Некорректная ссылка. Поддерживаются только github.com и stackoverflow.com\n"
                            + "Например: https://github.com/user/repo");
            return;
        }

        session.setPendingUrl(url);
        session.setState(UserState.WAITING_FOR_TAGS);
        telegramBotAdapter.sendMessage(
                chatId,
                "✅ Ссылка принята: " + url + "\n\n"
                        + "Введите теги через запятую (необязательно).\n"
                        + "Например: работа, баг\n\n"
                        + "Или отправьте /skip чтобы пропустить.");
    }

    private void handleTagsInput(long chatId, String input, UserSession session) {
        List<String> tags = List.of();
        if (!input.isBlank()) {
            tags = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isBlank())
                    .toList();
        }
        session.setPendingTags(tags);
        session.setState(UserState.WAITING_FOR_FILTERS);
        telegramBotAdapter.sendMessage(
                chatId, "Введите фильтры через запятую (необязательно).\n" + "Или отправьте /skip чтобы пропустить.");
    }

    private void handleFiltersInput(long chatId, String input, UserSession session) {
        List<String> filters = List.of();
        if (!input.isBlank()) {
            filters = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(f -> !f.isBlank())
                    .toList();
        }

        String url = session.getPendingUrl();
        List<String> tags = session.getPendingTags();
        session.reset();

        try {
            scrapperClient.registerChat(chatId);
            scrapperClient.addLink(chatId, url, tags, filters);
            log.atInfo()
                    .addKeyValue("url", url)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("tags", tags)
                    .addKeyValue("filters", filters)
                    .log("link.registered");
            String tagsInfo = tags.isEmpty() ? "" : "\n🏷 Теги: " + String.join(", ", tags);
            String filtersInfo = filters.isEmpty() ? "" : "\n🔍 Фильтры: " + String.join(", ", filters);
            telegramBotAdapter.sendMessage(
                    chatId, "✅ Ссылка добавлена в отслеживание:\n" + url + tagsInfo + filtersInfo);
        } catch (Exception e) {
            log.atWarn().addKeyValue("chatId", chatId).addKeyValue("url", url).log("link.register.failed", e);
            telegramBotAdapter.sendMessage(chatId, "❌ Не удалось добавить ссылку. Попробуйте позже.");
        }
    }

    private boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) return false;
            return host.equals("github.com") || host.equals("stackoverflow.com");
        } catch (Exception e) {
            return false;
        }
    }
}
