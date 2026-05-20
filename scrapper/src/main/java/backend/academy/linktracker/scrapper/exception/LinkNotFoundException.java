package backend.academy.linktracker.scrapper.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String url) {
        super("Link not found: " + url);
    }
}
