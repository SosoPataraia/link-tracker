package backend.academy.linktracker.scrapper.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListLinksResponse {
    private List<LinkResponse> links;
    private Integer size;
}
