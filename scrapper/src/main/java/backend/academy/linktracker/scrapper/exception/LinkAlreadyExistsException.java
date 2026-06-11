package backend.academy.linktracker.scrapper.exception;

public class LinkAlreadyExistsException extends RuntimeException {

    public LinkAlreadyExistsException(String url) {
        super("Link already tracked: " + url);
    }
}
