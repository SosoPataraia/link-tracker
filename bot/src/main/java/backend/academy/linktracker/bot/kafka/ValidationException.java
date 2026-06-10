package backend.academy.linktracker.bot.kafka;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
