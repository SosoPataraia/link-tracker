package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import java.util.List;
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
    private final LinkRepository linkRepository;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.interval:60000}")
    public void checkLinks() {
        int batchSize = schedulerProperties.getBatchSize();
        int offset = 0;
        int totalProcessed = 0;

        log.atInfo()
            .addKeyValue("batchSize", batchSize)
            .log("scheduler.start");

        while (true) {
            List<TrackedLink> batch = linkRepository.findBatch(offset, batchSize);

            if (batch.isEmpty()) {
                break;
            }

            log.atInfo()
                .addKeyValue("offset", offset)
                .addKeyValue("batchCount", batch.size())
                .log("scheduler.batch.processing");

            try {
                linkCheckerService.checkLinks(batch);
            } catch (Exception e) {
                log.error("Scheduler: error processing batch at offset={}", offset, e);
            }

            totalProcessed += batch.size();

            if (batch.size() < batchSize) {
                break;
            }

            offset += batchSize;
        }

        log.atInfo()
            .addKeyValue("totalProcessed", totalProcessed)
            .log("scheduler.complete");
    }
}
