package backend.academy.linktracker.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BotUpdate {
    private final long chatId;
    private final String text;
    private final String firstName;
    private final String username;
}
