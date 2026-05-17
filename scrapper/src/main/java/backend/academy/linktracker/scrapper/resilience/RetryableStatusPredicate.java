package backend.academy.linktracker.scrapper.resilience;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Component
@RequiredArgsConstructor
public class RetryableStatusPredicate implements Predicate<Throwable> {

    private final ResilienceProperties resilienceProperties;

    @Override
    public boolean test(Throwable throwable) {
        if (throwable instanceof HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            return resilienceProperties.getRetryableStatusCodes().contains(status);
        }
        return throwable instanceof java.io.IOException
                || throwable instanceof org.springframework.web.client.ResourceAccessException;
    }
}
