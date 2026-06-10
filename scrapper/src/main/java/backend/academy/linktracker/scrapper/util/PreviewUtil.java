package backend.academy.linktracker.scrapper.util;

public final class PreviewUtil {

    private static final int PREVIEW_LENGTH = 200;
    private static final String ELLIPSIS = "...";

    private PreviewUtil() {}

    /**
     * Trims leading whitespace and caps the text at {@value #PREVIEW_LENGTH} characters,
     * including the trailing ellipsis. Returns an empty string for null/blank input.
     */
    public static String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = text.stripLeading();
        if (stripped.length() <= PREVIEW_LENGTH) {
            return stripped;
        }
        return stripped.substring(0, PREVIEW_LENGTH - ELLIPSIS.length()) + ELLIPSIS;
    }
}
