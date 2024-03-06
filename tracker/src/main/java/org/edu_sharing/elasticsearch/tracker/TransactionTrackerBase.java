package org.edu_sharing.elasticsearch.tracker;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.alfresco.client.AlfrescoWebscriptClient;
import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.Transaction;
import org.edu_sharing.elasticsearch.alfresco.client.Transactions;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.elasticsearch.core.StatusIndexService;
import org.edu_sharing.elasticsearch.elasticsearch.core.WorkspaceService;
import org.edu_sharing.elasticsearch.elasticsearch.core.state.Tx;
import org.edu_sharing.elasticsearch.tracker.strategy.TrackerStrategy;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Slf4j
public abstract class TransactionTrackerBase implements TransactionTracker {

    @Getter
    protected final AlfrescoWebscriptClient alfClient;

    @Getter
    protected final WorkspaceService workspaceService;

    @Getter
    protected final EduSharingClient eduSharingClient;

    @Getter
    protected final StatusIndexService<Tx> transactionStateService;

    private final TrackerStrategy trackerStrategy;

    @Setter
    Integer threadCount = 4;

    @Setter
    int numberOfTransactions = 500;

    protected ForkJoinPool threadPool;

    protected TransactionTrackerBase(AlfrescoWebscriptClient alfClient, EduSharingClient eduSharingClient, WorkspaceService workspaceService, StatusIndexService<Tx> transactionStateService, TrackerStrategy trackerStrategy) {
        this.alfClient = alfClient;
        this.eduSharingClient = eduSharingClient;
        this.workspaceService = workspaceService;
        this.transactionStateService = transactionStateService;
        this.trackerStrategy = trackerStrategy;
    }

    public void init() {
        threadPool = new ForkJoinPool(threadCount);
    }

    @Override
    public boolean track() {
        try {
            eduSharingClient.refreshValuespaceCache();
            Tx txn = transactionStateService.getState();
            if (txn == null) {
                log.info("no transaction processed");
            }

            long lastTransactionId = Optional.ofNullable(txn).map(Tx::getTxnId).orElse(0L);
            log.info("starting lastTransactionId: {}", lastTransactionId);


            long nextTransactionId = lastTransactionId + 1;

            Transactions transactions = alfClient.getTransactions(nextTransactionId, trackerStrategy.getNext(nextTransactionId, numberOfTransactions), null, null, numberOfTransactions);

            long maxTrackerTxnId = transactions.getMaxTxnId();
            if (nextTransactionId > maxTrackerTxnId) {
                log.info("Tracker {} is up to date. maxTrackerTxnId: {}, lastTransactionId: {}", this.getClass().getSimpleName(), maxTrackerTxnId, lastTransactionId);
                return false;
            }

            if (transactions.getTransactions().isEmpty()) {
                if (
                        trackerStrategy.getLimit() != null &&
                        trackerStrategy.getNext(nextTransactionId, numberOfTransactions) >= trackerStrategy.getLimit()
                ) {
                    log.info("max transaction limit by strategy reached: {} / {}", maxTrackerTxnId, trackerStrategy.getLimit());
                    return false;
                } else if (maxTrackerTxnId <= trackerStrategy.getNext(lastTransactionId, numberOfTransactions)) {
                    log.info("index is up to date getMaxTxnId(): {}", maxTrackerTxnId);
                    return false;
                } else {
                    log.info("did not found new transactions in last transaction block min: {} max: {}", lastTransactionId, trackerStrategy.getNext(lastTransactionId, numberOfTransactions));
                    commit(transactionStateService,trackerStrategy.getNext(lastTransactionId, numberOfTransactions));
                    return true;
                }
            }

            List<Long> transactionIds = transactions.getTransactions()
                    .stream()
                    .map(Transaction::getId)
                    .collect(Collectors.toList());
            log.info("got " + transactionIds.size() + " transactions last:" + transactionIds.get(transactionIds.size() - 1));


            List<Node> nodes = alfClient.getNodes(transactionIds);
            log.info("got " + nodes.size() + " nodes");

            eduSharingClient.refreshValuespaceCache();

            // index nodes
            trackNodes(nodes);

            //remember processed transaction
            Long last = transactionIds.get(transactionIds.size() - 1);
            commit(transactionStateService, last);

            // log progress
            DecimalFormat df = new DecimalFormat("0.00");
            log.info("finished {}%, lastTransactionId: {} transactions: {} nodes: {} Stack size: {}",
                    df.format(calcProgress(transactions, transactionIds)),
                    last,
                    Arrays.toString(transactionIds.toArray()),
                    nodes.size(),
                    Thread.currentThread().getStackTrace().length);

            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private void commit(StatusIndexService<Tx> transactionStateService, long txId) throws IOException {
        log.info("safe transactionId {}", txId);
        transactionStateService.setState(new Tx(txId, 0L));
    }

    private Double calcProgress(Transactions transactions, List<Long> transactionIds) {
        Long last = transactionIds.get(transactionIds.size() - 1);
        return (double) last / (double) transactions.getMaxTxnId() * 100.0d;
    }

    public abstract void trackNodes(List<Node> nodes) throws IOException;
}
