package backend.academy.linktracker.ai.processor;

import backend.academy.linktracker.avro.RawUpdateEvent;

public interface UpdateProcessor {
    void process(RawUpdateEvent event);
}
