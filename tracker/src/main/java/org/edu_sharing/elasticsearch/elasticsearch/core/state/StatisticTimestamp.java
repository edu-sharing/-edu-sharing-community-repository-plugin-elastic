package org.edu_sharing.elasticsearch.elasticsearch.core.state;

import lombok.Data;


@Data
public class StatisticTimestamp {
    private long statisticTimestamp;
    private boolean allInIndex;

    // Required for deserialization
    public StatisticTimestamp() {

    }

    public StatisticTimestamp(boolean allInIndex, long statisticTimestamp) {
        this.statisticTimestamp = statisticTimestamp;
        this.allInIndex = allInIndex;
    }
}
