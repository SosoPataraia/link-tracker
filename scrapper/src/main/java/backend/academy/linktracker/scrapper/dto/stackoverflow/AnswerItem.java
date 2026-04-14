package backend.academy.linktracker.scrapper.dto.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnswerItem {

    @JsonProperty("answer_id")
    private Long answerId;

    @JsonProperty("creation_date")
    private Long creationDate;

    private String body;

    private OwnerInfo owner;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OwnerInfo {
        @JsonProperty("display_name")
        private String displayName;
    }
}
