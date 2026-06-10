package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkCheckerScheduler {

    private final LinkService linkService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval:60000}")
    public void checkLinks() {
        linkService.checkAllLinks();
    }
}
