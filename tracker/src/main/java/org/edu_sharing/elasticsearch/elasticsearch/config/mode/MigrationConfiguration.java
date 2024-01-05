package org.edu_sharing.elasticsearch.elasticsearch.config.mode;

import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationRunner;
import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(name = "mode", havingValue = "migration-only")
public class MigrationConfiguration {
    @Bean
    MigrationRunner migrationRunner(MigrationService migrationService){
        MigrationRunner migrationRunner = new MigrationRunner(migrationService);
        migrationRunner.setExitAfterExecution(true);
        return migrationRunner;
    }

}
