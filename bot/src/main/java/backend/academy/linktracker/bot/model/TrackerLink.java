package backend.academy.linktracker.bot.model;

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
@EqualsAndHashCode(of = "url")
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TrackedLink {

    private String url;
    private List<String> tags = new ArrayList<>();
}
