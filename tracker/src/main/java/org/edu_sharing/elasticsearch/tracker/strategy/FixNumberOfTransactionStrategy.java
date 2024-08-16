package org.edu_sharing.elasticsearch.tracker.strategy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FixNumberOfTransactionStrategy implements TrackerStrategy {
    @Override
    public Long getLimit() {
        return null;
    }
}
