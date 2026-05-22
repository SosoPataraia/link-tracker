package backend.academy.linktracker.scrapper.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkUpdate {
    private Long id;
    private String url;
    private String description;
    private List<Long> tgChatIds;
}
