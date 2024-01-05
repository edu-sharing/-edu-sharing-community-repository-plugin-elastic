package org.edu_sharing.elasticsearch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationCompletedAware;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class TransactionTrackerJob implements MigrationCompletedAware {

    private final TransactionTracker transactionTracker;
    private boolean migrated = false;

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track() {
        if (!migrated) {
            return;
        }

        boolean transactionChanges;
        do {
            transactionChanges = transactionTracker.track();
            log.info("recursive transactionChanges: {}", transactionChanges);
        } while (transactionChanges);
    }

    @Override
    public void MigrationCompleted() {
        migrated = true;
    }
}
