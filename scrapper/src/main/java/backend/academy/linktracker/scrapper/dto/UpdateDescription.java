package backend.academy.linktracker.scrapper.dto;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateDescription {

    public enum Type {
        NEW_ISSUE,
        NEW_PR,
        NEW_ANSWER,
        NEW_COMMENT
    }

    private final Type type;
    private final String questionTitle;
    private final String itemTitle;
    private final String username;
    private final Instant createdAt;
    private final String preview;

    private static final int PREVIEW_LENGTH = 200;

    public static String truncate(String text) {
        if (text == null || text.isBlank()) return "";
        String stripped = text.stripLeading();
        return stripped.length() <= PREVIEW_LENGTH ? stripped : stripped.substring(0, PREVIEW_LENGTH) + "...";
    }

    public String format(String url) {
        String typeLabel =
                switch (type) {
                    case NEW_ISSUE -> "🐛 New Issue";
                    case NEW_PR -> "🔀 New Pull Request";
                    case NEW_ANSWER -> "💬 New Answer";
                    case NEW_COMMENT -> "📝 New Comment";
                };

        var sb = new StringBuilder();
        sb.append(typeLabel).append("\n");
        sb.append("🔗 ").append(url).append("\n\n");

        if (questionTitle != null && !questionTitle.isBlank()) {
            sb.append("❓ ").append(questionTitle).append("\n");
        }
        if (itemTitle != null && !itemTitle.isBlank()) {
            sb.append("📌 ").append(itemTitle).append("\n");
        }

        sb.append("👤 ").append(username != null ? username : "unknown").append("\n");
        sb.append("🕐 ").append(createdAt).append("\n");

        if (!preview.isBlank()) {
            sb.append("\n").append(preview);
        }

        return sb.toString();
    }
}
