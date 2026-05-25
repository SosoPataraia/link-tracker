package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.avro.ProcessedUpdateEvent;
import java.util.function.Consumer;

public interface GroupingService {

    /**
     * Accepts a processed event for buffering.
     * If the grouping window for the contained tgChatIds has not yet expired,
     * the event is held. When the window closes, all buffered events for each
     * tgChatId are merged and emitted via the registered callback.
     *
     * @param event    the event to buffer
     * @param onEmit   callback invoked with the final (possibly grouped) event
     */
    void accept(ProcessedUpdateEvent event, Consumer<ProcessedUpdateEvent> onEmit);
}
