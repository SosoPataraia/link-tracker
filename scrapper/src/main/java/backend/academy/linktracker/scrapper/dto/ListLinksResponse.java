package backend.academy.linktracker.scrapper.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListLinksResponse implements Serializable {
    private List<LinkResponse> links;
    private Integer size;
}
