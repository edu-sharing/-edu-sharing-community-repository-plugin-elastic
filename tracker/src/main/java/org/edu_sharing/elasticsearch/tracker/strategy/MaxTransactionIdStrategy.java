package org.edu_sharing.elasticsearch.tracker.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MaxTransactionIdStrategy implements TrackerStrategy {

    private final long maxTransactionId;

    @Override
    public long getNext(long currentTransactionId, long maxTransactions) {
        return Math.min(maxTransactionId, currentTransactionId + maxTransactions);
    }
}
