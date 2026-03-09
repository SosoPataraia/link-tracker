package backend.academy.linktracker.scrapper.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(of = {"chatId", "url"})
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TrackedLink {

    private Long id;
    private long chatId;
    private String url;
    private List<String> tags = new ArrayList<>();
    private Instant lastChecked;
    private Instant lastUpdated;
}
