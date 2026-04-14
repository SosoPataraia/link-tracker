package backend.academy.linktracker.scrapper.dto.github;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IssueListResponse {
    private List<IssueItem> items;

    public static IssueListResponse of(List<IssueItem> items) {
        var r = new IssueListResponse();
        r.items = items;
        return r;
    }
}
