package backend.academy.linktracker.scrapper.dto.stackoverflow;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QuestionResponse {
    private List<QuestionItem> items;
}
