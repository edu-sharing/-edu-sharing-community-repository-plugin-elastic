package org.edu_sharing.elasticsearch.elasticsearch.core.migration;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@RequiredArgsConstructor
public class MigrationRunner implements ApplicationRunner {

    private final MigrationService migrationService;

    @Setter
    private boolean exitAfterExecution;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        migrationService.runMigration();
        if(exitAfterExecution) {
            System.exit(0);
        }
    }
}
