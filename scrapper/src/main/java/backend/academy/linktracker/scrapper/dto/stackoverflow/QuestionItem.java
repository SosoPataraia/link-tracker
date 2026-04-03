package backend.academy.linktracker.scrapper.dto.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuestionItem {

    @JsonProperty("question_id")
    private Long questionId;

    @JsonProperty("last_activity_date")
    private Long lastActivityDate;

    private String title;
}
