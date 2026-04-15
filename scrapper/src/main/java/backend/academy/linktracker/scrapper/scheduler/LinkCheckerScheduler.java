package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkCheckerService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
        int threadCount = schedulerProperties.getThreadCount();
        int offset = 0;
        int totalProcessed = 0;
        List<String> allFailedUrls = new ArrayList<>();

        log.atInfo()
                .addKeyValue("batchSize", batchSize)
                .addKeyValue("threadCount", threadCount)
                .log("scheduler.start");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            while (true) {
                List<TrackedLink> batch = linkRepository.findBatch(offset, batchSize);
                if (batch.isEmpty()) break;

                log.atInfo()
                        .addKeyValue("offset", offset)
                        .addKeyValue("batchCount", batch.size())
                        .log("scheduler.batch.processing");

                List<String> failures = processBatchParallel(batch, threadCount, executor);
                allFailedUrls.addAll(failures);
                totalProcessed += batch.size();

                if (batch.size() < batchSize) break;
                offset += batchSize;
            }
        } finally {
            executor.shutdown();
        }

        if (!allFailedUrls.isEmpty()) {
            log.atWarn()
                    .addKeyValue("failedCount", allFailedUrls.size())
                    .addKeyValue("failedUrls", allFailedUrls)
                    .log("scheduler.batch.failures");
        }

        log.atInfo()
                .addKeyValue("totalProcessed", totalProcessed)
                .addKeyValue("failedCount", allFailedUrls.size())
                .log("scheduler.complete");
    }

    /**
     * Splits the batch into {@code threadCount} sublists, submits each to the executor,
     * waits for all to finish, collects failed URLs.
     */
    private List<String> processBatchParallel(List<TrackedLink> batch, int threadCount, ExecutorService executor) {

        List<List<TrackedLink>> sublists = partition(batch, threadCount);
        List<Future<List<String>>> futures = new ArrayList<>();

        for (List<TrackedLink> sublist : sublists) {
            futures.add(executor.submit(() -> processSublist(sublist)));
        }

        List<String> failures = new ArrayList<>();
        for (Future<List<String>> future : futures) {
            try {
                failures.addAll(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Scheduler thread interrupted", e);
            } catch (ExecutionException e) {
                log.error("Unexpected error in scheduler thread", e.getCause());
            }
        }
        return failures;
    }

    /**
     * Processes one sublist. Each URL's failure is isolated — others continue.
     * Returns list of URLs that failed.
     */
    private List<String> processSublist(List<TrackedLink> links) {
        List<String> failures = new ArrayList<>();

        var byUrl = links.stream().collect(Collectors.groupingBy(TrackedLink::getUrl));

        byUrl.forEach((url, subscribers) -> {
            try {
                linkCheckerService.checkLinks(subscribers);
            } catch (Exception e) {
                log.error("Failed to process url={}: {}", url, e.getMessage(), e);
                failures.add(url);
            }
        });

        return failures;
    }

    /**
     * Splits list into at most {@code n} roughly equal sublists.
     */
    private <T> List<List<T>> partition(List<T> list, int n) {
        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        int chunkSize = Math.max(1, (int) Math.ceil((double) size / n));
        for (int i = 0; i < size; i += chunkSize) {
            result.add(list.subList(i, Math.min(i + chunkSize, size)));
        }
        return result;
    }
}
