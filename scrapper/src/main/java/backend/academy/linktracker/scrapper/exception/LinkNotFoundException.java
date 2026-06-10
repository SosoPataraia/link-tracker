package backend.academy.linktracker.scrapper.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(long chatId, String url) {
        super("Link not found: chatId=" + chatId + " url=" + url);
    }
}
