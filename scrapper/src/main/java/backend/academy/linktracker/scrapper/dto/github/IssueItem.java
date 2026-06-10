package backend.academy.linktracker.scrapper.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IssueItem {

    private Long number;

    private String title;

    private String body;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("pull_request")
    private Object pullRequest;

    private UserInfo user;

    public boolean isPullRequest() {
        return pullRequest != null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UserInfo {
        private String login;
    }

    public void setPullRequest(Object pullRequest) {
        this.pullRequest = pullRequest;
    }
}
