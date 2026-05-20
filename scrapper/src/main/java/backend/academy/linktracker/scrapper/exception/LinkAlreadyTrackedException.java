package backend.academy.linktracker.scrapper.exception;

public class LinkAlreadyTrackedException extends RuntimeException {
    public LinkAlreadyTrackedException(String url) {
        super("Link already tracked: " + url);
    }
}
