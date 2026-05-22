package backend.academy.linktracker.scrapper.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkResponse implements Serializable {
    private Long id;
    private String url;
    private List<String> tags;
}
