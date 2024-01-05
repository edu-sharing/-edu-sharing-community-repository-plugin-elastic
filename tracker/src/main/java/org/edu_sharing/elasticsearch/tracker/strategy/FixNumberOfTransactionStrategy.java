package org.edu_sharing.elasticsearch.tracker.strategy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FixNumberOfTransactionStrategy implements TrackerStrategy {

    @Override
    public long getNext(long nextTransactionId, long maxTransactions) {
        return nextTransactionId + maxTransactions;
    }
}
