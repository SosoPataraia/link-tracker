package backend.academy.linktracker.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.BotCommand;
import backend.academy.linktracker.bot.command.CancelCommand;
import backend.academy.linktracker.bot.command.ListCommand;
import backend.academy.linktracker.bot.command.StartCommand;
import backend.academy.linktracker.bot.command.TrackCommand;
import backend.academy.linktracker.bot.command.UntrackCommand;
import backend.academy.linktracker.bot.handler.TelegramUpdateHandler;
import backend.academy.linktracker.bot.repository.InMemoryLinkRepository;
import backend.academy.linktracker.bot.repository.SessionRepository;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramUpdateHandlerTest {

    @Mock
    TelegramBot telegramBot;

    @Mock
    ScrapperClient scrapperClient;

    @Mock
    SendResponse sendResponse;

    SessionRepository sessionRepository;
    InMemoryLinkRepository linkRepository;
    TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        sessionRepository = new SessionRepository();
        linkRepository = new InMemoryLinkRepository();
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(sendResponse);

        List<BotCommand> commands = List.of(
                new StartCommand(telegramBot),
                new TrackCommand(telegramBot, sessionRepository),
                new UntrackCommand(telegramBot, linkRepository, sessionRepository, scrapperClient),
                new ListCommand(telegramBot, linkRepository, sessionRepository),
                new CancelCommand(telegramBot, sessionRepository));

        handler = new TelegramUpdateHandler(telegramBot, sessionRepository, linkRepository, scrapperClient, commands);
    }

    @Test
    void trackCommand_setsStateToWaitingForLink() {
        sendUpdate(100L, "/track");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_LINK);
    }

    @Test
    void validUrlAfterTrack_setsStateToWaitingForTags() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_TAGS);
        assertThat(sessionRepository.get(100L).getPendingUrl()).isEqualTo("https://github.com/user/repo");
    }

    @Test
    void invalidUrlAfterTrack_staysInWaitingForLink() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "notaurl");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_LINK);
    }

    @Test
    void tagsInput_savesLinkWithTags() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        sendUpdate(100L, "work, java");

        var links = linkRepository.findAllByChat(100L);
        assertThat(links).hasSize(1);
        assertThat(links.getFirst().getTags()).contains("work", "java");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.IDLE);
    }

    @Test
    void duplicateLink_resetsStateAndNotifiesUser() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        sendUpdate(100L, "");

        // Try to add same link again
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");

        // Duplicate detected — state reset to IDLE
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.IDLE);
        // Only one link stored
        assertThat(linkRepository.findAllByChat(100L)).hasSize(1);
    }

    @Test
    void cancelCommand_resetsState() {
        sendUpdate(100L, "/track");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_LINK);

        sendUpdate(100L, "/cancel");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.IDLE);
    }

    @Test
    void newCommandDuringDialog_cancelsDialog() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "/list");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.IDLE);
    }

    @Test
    void listCommand_callsTelegramBot() {
        sendUpdate(100L, "/list");
        verify(telegramBot).execute(any(SendMessage.class));
    }

    @Test
    void unknownCommand_sendsNotification() {
        sendUpdate(100L, "/unknowncommand");
        verify(telegramBot).execute(any(SendMessage.class));
    }

    private void sendUpdate(long chatId, String text) {
        handler.processUpdate(mockUpdate(chatId, text));
    }

    private Update mockUpdate(long chatId, String text) {
        var update = Mockito.mock(Update.class);
        var message = Mockito.mock(Message.class);
        var chat = Mockito.mock(Chat.class);
        var user = Mockito.mock(User.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);
        when(chat.id()).thenReturn(chatId);
        when(user.firstName()).thenReturn("Test");
        when(user.username()).thenReturn("testuser");
        return update;
    }
}
