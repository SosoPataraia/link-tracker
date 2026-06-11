package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.Collection;

public interface LinkCheckerService {

    void checkLinks(Collection<TrackedLink> links);
}
