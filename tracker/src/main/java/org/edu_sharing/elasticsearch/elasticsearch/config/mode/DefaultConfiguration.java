package org.edu_sharing.elasticsearch.elasticsearch.config.mode;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationService;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.WaitForMigrationJob;
import org.edu_sharing.elasticsearch.jobs.AclTrackerJob;
import org.edu_sharing.elasticsearch.jobs.StatisticsTrackerJob;
import org.edu_sharing.elasticsearch.jobs.TransactionTrackerJob;
import org.edu_sharing.elasticsearch.tracker.AclTracker;
import org.edu_sharing.elasticsearch.tracker.StatisticsTracker;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mode", havingValue = "default", matchIfMissing = true)
public class DefaultConfiguration {

    @Bean
    public WaitForMigrationJob waitForMigrationJob(MigrationService migrationService){
        return new WaitForMigrationJob(migrationService);
    }

    @Bean
    public TransactionTrackerJob transactionTrackerJob(TransactionTracker transactionTracker){
        return new TransactionTrackerJob(transactionTracker);
    }

    @Bean
    public AclTrackerJob aclTrackerJob(AclTracker aclTracker){
        return new AclTrackerJob(aclTracker);
    }

    @Bean
    @ConditionalOnProperty(prefix = "statistic", name = "enabled", havingValue = "true")
    public StatisticsTrackerJob statisticsTrackerJob(StatisticsTracker statisticsTracker){
        return new StatisticsTrackerJob(statisticsTracker);
    }
}
