package backend.academy.linktracker.bot.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.HelpCommand;
import backend.academy.linktracker.bot.command.StartCommand;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandHandlerTest {

    private CommandHandler commandHandler;

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Mock
    private Chat chat;

    private static final long CHAT_ID = 123456789L;

    @BeforeEach
    void setUp() {
        var startCommand = new StartCommand();
        var helpCommand = new HelpCommand(List.of(startCommand, new HelpCommand(List.of())));
        commandHandler = new CommandHandler(List.of(startCommand, helpCommand));
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(CHAT_ID);
    }

    @Test
    void shouldReturnWelcomeMessageOnStartCommand() {
        when(message.text()).thenReturn("/start");

        SendMessage response = commandHandler.handleCommand(update);

        assertThat(response).isNotNull();
        assertThat(getResponseText(response)).containsIgnoringCase("Добро пожаловать");
    }

    @Test
    void shouldReturnHelpMessageOnHelpCommand() {
        when(message.text()).thenReturn("/help");

        SendMessage response = commandHandler.handleCommand(update);

        assertThat(response).isNotNull();
        assertThat(getResponseText(response)).contains("/start").contains("/help");
    }

    @Test
    void shouldReturnErrorMessageOnUnknownCommand() {
        when(message.text()).thenReturn("/unknown");

        SendMessage response = commandHandler.handleCommand(update);

        assertThat(response).isNotNull();
        assertThat(getResponseText(response)).containsIgnoringCase("Неизвестная команда");
    }

    @Test
    void shouldReturnErrorMessageOnArbitraryText() {
        when(message.text()).thenReturn("hello world");

        SendMessage response = commandHandler.handleCommand(update);

        assertThat(response).isNotNull();
        assertThat(getResponseText(response)).containsIgnoringCase("Неизвестная команда");
    }

    private String getResponseText(SendMessage sendMessage) {
        return sendMessage.getParameters().get("text").toString();
    }
}
