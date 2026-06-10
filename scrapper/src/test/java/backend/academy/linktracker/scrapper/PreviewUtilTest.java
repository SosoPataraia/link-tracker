package backend.academy.linktracker.scrapper;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.util.PreviewUtil;
import org.junit.jupiter.api.Test;

class PreviewUtilTest {

    @Test
    void truncate_shorterThanLimit_returnsAsIs() {
        assertThat(PreviewUtil.truncate("Short text")).isEqualTo("Short text");
    }

    @Test
    void truncate_exactlyLimit_returnsAsIs() {
        String input = "A".repeat(200);
        assertThat(PreviewUtil.truncate(input)).isEqualTo(input);
    }

    @Test
    void truncate_longerThanLimit_capsAt200IncludingEllipsis() {
        String result = PreviewUtil.truncate("A".repeat(300));
        assertThat(result).hasSize(200);
        assertThat(result).endsWith("...");
        assertThat(result).startsWith("A".repeat(197));
    }

    @Test
    void truncate_null_returnsEmpty() {
        assertThat(PreviewUtil.truncate(null)).isEmpty();
    }

    @Test
    void truncate_blank_returnsEmpty() {
        assertThat(PreviewUtil.truncate("   ")).isEmpty();
    }

    @Test
    void truncate_stripsLeadingWhitespace() {
        assertThat(PreviewUtil.truncate("   hello")).isEqualTo("hello");
    }
}
