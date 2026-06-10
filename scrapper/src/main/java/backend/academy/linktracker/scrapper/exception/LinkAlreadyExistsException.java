package backend.academy.linktracker.scrapper.exception;

public class LinkAlreadyExistsException extends RuntimeException {
    public LinkAlreadyExistsException(long chatId, String url) {
        super("Link already exists: chatId=" + chatId + " url=" + url);
    }
}
