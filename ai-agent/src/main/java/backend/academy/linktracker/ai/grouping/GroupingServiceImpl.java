package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.ai.config.AiAgentProperties;
import backend.academy.linktracker.ai.priority.Priority;
import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Buffers {@link ProcessedUpdateEvent}s per tgChatId.
 * The first event for a given chatId opens a window of {@code window-ms} milliseconds.
 * When the window closes, all buffered events for that chatId are merged into one
 * (numbered list in description, max priority) and emitted.
 *
 * <p>An event may address multiple tgChatIds; each chatId is buffered independently.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupingServiceImpl implements GroupingService {

    private final AiAgentProperties properties;

    /**
     * Buffer: chatId → list of events waiting to be flushed.
     */
    private final ConcurrentHashMap<Long, List<ProcessedUpdateEvent>> buffer = new ConcurrentHashMap<>();

    /**
     * Tracks the scheduled flush task per chatId so we don't double-schedule.
     */
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledFlushes = new ConcurrentHashMap<>();

    /**
     * Single-threaded scheduler — window expiry tasks are lightweight.
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "grouping-flush");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void accept(ProcessedUpdateEvent event, Consumer<ProcessedUpdateEvent> onEmit) {
        for (Long chatId : event.getTgChatIds()) {
            bufferForChat(chatId, event, onEmit);
        }
    }

    private void bufferForChat(long chatId, ProcessedUpdateEvent event, Consumer<ProcessedUpdateEvent> onEmit) {
        buffer.computeIfAbsent(chatId, k -> new ArrayList<>()).add(event);

        scheduledFlushes.computeIfAbsent(
                chatId,
                k -> scheduler.schedule(
                        () -> flush(chatId, onEmit), properties.grouping().windowMs(), TimeUnit.MILLISECONDS));

        log.atDebug()
                .addKeyValue("chatId", chatId)
                .addKeyValue("bufferSize", buffer.get(chatId).size())
                .log("grouping.buffered");
    }

    private void flush(long chatId, Consumer<ProcessedUpdateEvent> onEmit) {
        List<ProcessedUpdateEvent> events = buffer.remove(chatId);
        scheduledFlushes.remove(chatId);

        if (events == null || events.isEmpty()) {
            return;
        }

        ProcessedUpdateEvent merged = events.size() == 1 ? events.getFirst() : merge(chatId, events);

        log.atInfo()
                .addKeyValue("chatId", chatId)
                .addKeyValue("count", events.size())
                .addKeyValue("priority", merged.getPriority())
                .log("grouping.flushed");

        onEmit.accept(merged);
    }

    /**
     * Merges multiple events for the same chatId into one:
     * <ul>
     *   <li>description → numbered list</li>
     *   <li>priority → maximum of all priorities</li>
     *   <li>tgChatIds → singleton list with this chatId</li>
     *   <li>id → id of the first event</li>
     * </ul>
     */
    private ProcessedUpdateEvent merge(long chatId, List<ProcessedUpdateEvent> events) {
        var sb = new StringBuilder();
        backend.academy.linktracker.avro.Priority maxPriority =
                events.getFirst().getPriority();

        for (int i = 0; i < events.size(); i++) {
            ProcessedUpdateEvent e = events.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(e.getDescription() != null ? e.getDescription().toString() : "")
                    .append("\n");

            Priority domain = Priority.valueOf(
                    e.getPriority() == backend.academy.linktracker.avro.Priority.NORMAL
                            ? "MEDIUM"
                            : e.getPriority().name());
            Priority currentMax = Priority.valueOf(
                    maxPriority == backend.academy.linktracker.avro.Priority.NORMAL ? "MEDIUM" : maxPriority.name());
            maxPriority = Priority.max(currentMax, domain).toAvro();
        }

        return ProcessedUpdateEvent.newBuilder()
                .setId(events.getFirst().getId())
                .setDescription(sb.toString().trim())
                .setTgChatIds(List.of(chatId))
                .setPriority(maxPriority)
                .build();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
