package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LinkCheckerScheduler {

    private final LinkCheckerService linkCheckerService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval:60000}")
    public void checkLinks() {
        log.info("Scheduler: starting link check");
        try {
            linkCheckerService.checkAllLinks();
        } catch (Exception e) {
            log.error("Scheduler: error during link check", e);
        }
        log.info("Scheduler: link check complete");
    }
}
