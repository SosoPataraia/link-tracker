package backend.academy.linktracker.ai.priority;

/**
 * Domain-level priority, independent of the Avro-generated enum.
 * Mapping to Avro happens only at the Kafka producer boundary.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH;

    /**
     * Returns the higher-priority value of the two.
     * HIGH > MEDIUM > LOW.
     */
    public static Priority max(Priority a, Priority b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /**
     * Maps this domain priority to the Avro-generated Priority enum.
     */
    public backend.academy.linktracker.avro.Priority toAvro() {
        return switch (this) {
            case HIGH -> backend.academy.linktracker.avro.Priority.HIGH;
            case MEDIUM -> backend.academy.linktracker.avro.Priority.NORMAL;
            case LOW -> backend.academy.linktracker.avro.Priority.LOW;
        };
    }
}
