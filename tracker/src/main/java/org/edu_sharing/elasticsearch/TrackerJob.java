package org.edu_sharing.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.tracker.ACLTracker;
import org.edu_sharing.elasticsearch.tracker.StatisticsTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTrackerInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrackerJob {

    @Value("${statistic.enabled}")
    boolean statisticEnabled;

    private final TransactionTrackerInterface transactionTracker;
    private final ACLTracker aclTracker;
    private final StatisticsTracker statisticsTracker;


    public TrackerJob(TransactionTrackerInterface transactionTracker, ACLTracker aclTracker, StatisticsTracker statisticsTracker) {
        this.transactionTracker = transactionTracker;
        this.aclTracker = aclTracker;
        this.statisticsTracker = statisticsTracker;
    }

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track(){
        boolean aclChanges=aclTracker.track();
        boolean transactionChanges=transactionTracker.track();

        if(aclChanges || transactionChanges){
            log.info("recursive aclChanges: {} -- transactionChanges: {}", aclChanges, transactionChanges);
            track();
        }
    }

    /**
     * no race condition possibe with track() cause all scheduled tasks are executed by single thread
     * <a href="https://stackoverflow.com/questions/24033208/how-to-prevent-overlapping-schedules-in-spring">...</a>
     */
    @Scheduled(fixedDelayString = "${statistic.delay}")
    public void trackStatistics(){
        if(statisticEnabled) {
            statisticsTracker.track();
        }
    }
}
