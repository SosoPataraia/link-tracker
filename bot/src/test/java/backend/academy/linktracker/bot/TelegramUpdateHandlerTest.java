package backend.academy.linktracker.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.BotCommand;
import backend.academy.linktracker.bot.command.CancelCommand;
import backend.academy.linktracker.bot.command.HelpCommand;
import backend.academy.linktracker.bot.command.ListCommand;
import backend.academy.linktracker.bot.command.StartCommand;
import backend.academy.linktracker.bot.command.TrackCommand;
import backend.academy.linktracker.bot.command.UntrackCommand;
import backend.academy.linktracker.bot.handler.TelegramUpdateHandler;
import backend.academy.linktracker.bot.repository.InMemorySessionRepository;
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
    TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        sessionRepository = new InMemorySessionRepository();
        when(telegramBot.execute(any(SendMessage.class))).thenReturn(sendResponse);

        List<BotCommand> commands = List.of(
                new StartCommand(telegramBot),
                new TrackCommand(telegramBot, sessionRepository),
                new UntrackCommand(telegramBot, scrapperClient, sessionRepository),
                new ListCommand(telegramBot, scrapperClient, sessionRepository),
                new CancelCommand(telegramBot, sessionRepository),
                new HelpCommand(telegramBot, List.of()));

        handler = new TelegramUpdateHandler(telegramBot, sessionRepository, scrapperClient, commands);
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
    void nonSupportedHostUrl_staysInWaitingForLink() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://example.com/something");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_LINK);
    }

    @Test
    void tagsInput_movesToWaitingForFilters() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        sendUpdate(100L, "work, java");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_FILTERS);
        assertThat(sessionRepository.get(100L).getPendingTags()).contains("work", "java");
    }

    @Test
    void skipTags_movesToWaitingForFilters() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        sendUpdate(100L, "/skip");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_FILTERS);
    }

    @Test
    void filtersInput_completesRegistration() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        sendUpdate(100L, "work");
        sendUpdate(100L, "open, bug");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.IDLE);
        verify(scrapperClient).addLink(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    void skipFilters_completesRegistration() {
        sendUpdate(100L, "/track");
        sendUpdate(100L, "https://github.com/user/repo");
        sendUpdate(100L, "/skip");
        sendUpdate(100L, "/skip");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.IDLE);
        verify(scrapperClient).addLink(anyLong(), anyString(), anyList(), anyList());
    }

    @Test
    void cancelCommand_resetsState() {
        sendUpdate(100L, "/track");
        assertThat(sessionRepository.get(100L).getState()).isEqualTo(UserState.WAITING_FOR_LINK);
        sendUpdate(100L, "/cancel");
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
        handler.handleUpdate(mockUpdate(chatId, text));
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
