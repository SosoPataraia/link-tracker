package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.Collection;
import java.util.List;

public interface LinkCheckerService {

    /**
     * Checks the given links and sends updates. Groups links by URL so each
     * external API is called once per unique URL. A failure on one URL does
     * not affect the others.
     *
     * @return list of URLs that could not be processed
     */
    List<String> checkLinks(Collection<TrackedLink> links);
}
