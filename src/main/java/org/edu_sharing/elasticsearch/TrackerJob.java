package org.edu_sharing.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edu_sharing.elasticsearch.tracker.ACLTracker;
import org.edu_sharing.elasticsearch.tracker.StatisticsTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrackerJob {

    @Value("${statistic.enabled}")
    boolean statisticEnabled;

    @Autowired
    TransactionTracker transactionTracker;

    @Autowired
    ACLTracker aclTracker;

    @Autowired
    StatisticsTracker statisticsTracker;

    Logger logger = LogManager.getLogger(TrackerJob.class);

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track(){
        boolean aclChanges=aclTracker.track();
        boolean transactionChanges=transactionTracker.track();

        if(aclChanges || transactionChanges){
            logger.info("recursiv aclChanges:" + aclChanges +" transactionChanges:"+transactionChanges);
            track();
        }
    }

    /**
     * no race condition possibe with track() cause all scheduled tasks are executed by single thread
     * https://stackoverflow.com/questions/24033208/how-to-prevent-overlapping-schedules-in-spring
     */
    @Scheduled(fixedDelayString = "${statistic.delay}")
    public void trackStatistics(){
        if(statisticEnabled) {
            statisticsTracker.track();
        }
    }
}
