package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramConfiguration {

    @Bean
    public TelegramBot telegramBot(TelegramProperties properties) {
        var builder = new TelegramBot.Builder(properties.getToken())
                .updateListenerSleep(properties.getUpdateListenerSleep().toMillis());

        String url = properties.getUrl();
        if (url != null && !url.isBlank() && !url.equals("https://api.telegram.org/")) {
            builder.apiUrl(url);
        }

        if (properties.isDebug()) {
            builder.debug();
        }

        return builder.build();
    }
}
