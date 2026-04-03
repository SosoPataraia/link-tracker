package backend.academy.linktracker.bot.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddLinkRequest {
    private String link;
    private List<String> tags;
    private List<String> filters;
}
