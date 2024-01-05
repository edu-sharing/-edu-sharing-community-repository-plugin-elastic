package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.elasticsearch.core.migration.MigrationInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class Migrations {

    @Bean
    @Order(0)
    public MigrationInfo migration9_0() {
        return new MigrationInfo("9.0", true);
    }
}
