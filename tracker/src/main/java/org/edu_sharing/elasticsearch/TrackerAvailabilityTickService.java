package org.edu_sharing.elasticsearch;

public interface TrackerAvailabilityTickService {

    /**
     * tell the application that the tracker is still alive and has just triggered a new run
     * Used for liveness probes
     */
    void tick();
}
