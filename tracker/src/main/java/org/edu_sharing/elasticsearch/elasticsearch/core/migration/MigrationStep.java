package org.edu_sharing.elasticsearch.elasticsearch.core.migration;

public enum MigrationStep {
    INIT_PROGRESS_STEP(0, "Not Started"),
    REINDEX_WORKSPACE_INDEX_PROGRESS_STEP(1, "Reindex Workspace"),
    REINDEX_TRANSACTIONS_INDEX_PROGRESS_STEP(2, "Reindex transactions"),
    MIGRATE_DOCUMENTS_PROGRESS_STEP(3, "Migrate Documents"),
    COMPLETED_PROGRESS_STEP(4, "Completed");


    public final int value;
    public final String message;

    MigrationStep(int value, String message) {
        this.value = value;
        this.message = message;
    }

    public static MigrationStep valueOf(int value) {
        switch (value){
            case 0: return INIT_PROGRESS_STEP;
            case 1: return REINDEX_WORKSPACE_INDEX_PROGRESS_STEP;
            case 2: return REINDEX_TRANSACTIONS_INDEX_PROGRESS_STEP;
            case 3: return MIGRATE_DOCUMENTS_PROGRESS_STEP;
            case 4: return COMPLETED_PROGRESS_STEP;
            default: throw new IllegalArgumentException(value + " is not an valid value");
        }
    }
}
