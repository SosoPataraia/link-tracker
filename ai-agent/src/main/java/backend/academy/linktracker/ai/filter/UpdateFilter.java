package backend.academy.linktracker.ai.filter;

import backend.academy.linktracker.avro.RawUpdateEvent;

public interface UpdateFilter {
    FilterResult apply(RawUpdateEvent event);
}
