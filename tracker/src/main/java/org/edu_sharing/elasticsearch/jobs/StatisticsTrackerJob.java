package org.edu_sharing.elasticsearch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.TrackerAvailabilityService;
import org.edu_sharing.elasticsearch.TrackerAvailabilityTickService;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationCompletedAware;
import org.edu_sharing.elasticsearch.tracker.StatisticsTracker;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class StatisticsTrackerJob implements MigrationCompletedAware {

    private final StatisticsTracker statisticsTracker;
    private final TrackerAvailabilityTickService tickService;

    private boolean migrated = false;

    /**
     * no race condition possibe with track() cause all scheduled tasks are executed by single thread
     * <a href="https://stackoverflow.com/questions/24033208/how-to-prevent-overlapping-schedules-in-spring">...</a>
     */
    @Scheduled(fixedDelayString = "${statistic.delay}")
    public void track() {
        tickService.tick();
        if (!migrated) {
            return;
        }

        statisticsTracker.track();
    }

    @Override
    public void MigrationCompleted() {
        migrated = true;
    }
}
