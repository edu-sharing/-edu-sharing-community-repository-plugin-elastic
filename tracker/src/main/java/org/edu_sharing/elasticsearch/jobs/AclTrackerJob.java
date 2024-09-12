package org.edu_sharing.elasticsearch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.TrackerAvailabilityTickService;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationCompletedAware;
import org.edu_sharing.elasticsearch.tracker.AclTracker;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class AclTrackerJob implements MigrationCompletedAware {

    private final AclTracker aclTracker;
    private final TrackerAvailabilityTickService tickService;

    private boolean migrated = false;


    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track() {
        tickService.tick();
        if (!migrated) {
            return;
        }

        boolean aclChanges;
        do {
            aclChanges = aclTracker.track();
            log.info("recursive aclChanges: {}", aclChanges);
        } while (aclChanges);
    }

    @Override
    public void MigrationCompleted() {
        migrated = true;
    }
}
