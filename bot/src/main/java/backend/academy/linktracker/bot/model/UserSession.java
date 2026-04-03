package backend.academy.linktracker.bot.model;

import backend.academy.linktracker.bot.state.UserState;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSession {

    private UserState state = UserState.IDLE;
    private String pendingUrl;
    private List<String> pendingTags = List.of();

    public void reset() {
        this.state = UserState.IDLE;
        this.pendingUrl = null;
        this.pendingTags = List.of();
    }
}
