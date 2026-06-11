package backend.academy.linktracker.scrapper.resilience;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import java.io.IOException;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

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
        return throwable instanceof IOException || throwable instanceof ResourceAccessException;
    }
}
