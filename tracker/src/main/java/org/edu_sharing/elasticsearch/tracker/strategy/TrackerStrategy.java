package org.edu_sharing.elasticsearch.tracker.strategy;

public interface TrackerStrategy {
    long getNext(long nextTransactionId, long maxTransactions);
}
