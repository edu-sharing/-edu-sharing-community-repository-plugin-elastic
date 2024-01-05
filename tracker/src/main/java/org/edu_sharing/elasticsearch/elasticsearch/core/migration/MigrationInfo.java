package org.edu_sharing.elasticsearch.elasticsearch.core.migration;

import lombok.Value;

@Value
public class MigrationInfo {
    String version;
    boolean requiresReindex;
}
