package org.edu_sharing.elasticsearch.jobs;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.TrackerAvailabilityService;
import org.edu_sharing.elasticsearch.TrackerAvailabilityTickService;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationCompletedAware;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class TransactionTrackerJob implements MigrationCompletedAware, ApplicationContextAware {

    private final TransactionTracker transactionTracker;
    private final TrackerAvailabilityTickService tickService;

    private boolean migrated = false;

    @Value("${shutdown.on.exception}")
    boolean shutDownOnException = true;

    @Setter
    private ApplicationContext applicationContext;

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track() {
        tickService.tick();
        if (!migrated) {
            return;
        }

        boolean transactionChanges;
        do {
            transactionChanges = false;
            try {
                transactionChanges = transactionTracker.track();
                log.info("recursive transactionChanges: {}", transactionChanges);
            }catch (Throwable e){
                log.error(e.getMessage(),e);
                if((e instanceof OutOfMemoryError) && shutDownOnException){
                    log.info("will shutdown tracker cause of exception: {}", e.getMessage(), e);
                    ((ConfigurableApplicationContext) applicationContext).close();
                }
            }
        } while (transactionChanges);
    }

    @Override
    public void MigrationCompleted() {
        migrated = true;
    }
}
