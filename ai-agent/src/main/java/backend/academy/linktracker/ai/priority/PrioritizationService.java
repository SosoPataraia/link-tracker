package backend.academy.linktracker.ai.priority;

public interface PrioritizationService {
    /**
     * Determines the priority of an update based on keywords in its description.
     *
     * @param description the update text (may be null)
     * @return HIGH if any high-keyword found, LOW if any low-keyword found (and no high),
     *         MEDIUM otherwise
     */
    Priority determine(String description);
}
