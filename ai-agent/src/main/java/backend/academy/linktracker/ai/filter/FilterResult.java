package backend.academy.linktracker.ai.filter;

public record FilterResult(boolean passed, String reason) {

    public static FilterResult pass() {
        return new FilterResult(true, null);
    }

    public static FilterResult reject(String reason) {
        return new FilterResult(false, reason);
    }
}
