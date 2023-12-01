package org.edu_sharing.elasticsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.edu_sharing.elasticsearch.tracker.ACLTracker;
import org.edu_sharing.elasticsearch.tracker.StatisticsTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTrackerInterface;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TrackerJob implements ApplicationContextAware {

    @Value("${statistic.enabled}")
    boolean statisticEnabled;

    @Value("${shutdown.on.exception}")
    boolean shutDownOnException = true;

    @Autowired
    TransactionTrackerInterface transactionTracker;

    @Autowired
    ACLTracker aclTracker;

    @Autowired
    StatisticsTracker statisticsTracker;

    Logger logger = LogManager.getLogger(TrackerJob.class);

    private ApplicationContext context;

    @Scheduled(fixedDelayString = "${tracker.delay}")
    public void track(){
        try{
            boolean aclChanges=aclTracker.track();
            boolean transactionChanges=transactionTracker.track();

            if(aclChanges || transactionChanges){
                logger.info("recursiv aclChanges:" + aclChanges +" transactionChanges:"+transactionChanges);
                track();
            }
        }catch(Throwable e){
            logger.error(e.getMessage(),e);

            if((e instanceof OutOfMemoryError) && shutDownOnException){
                logger.info("will shutdown tracker cause of exception:" + e.getMessage());
                ((ConfigurableApplicationContext) context).close();
            }
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
