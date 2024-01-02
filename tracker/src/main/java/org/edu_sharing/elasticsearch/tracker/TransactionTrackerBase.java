package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.AlfrescoWebscriptClient;
import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.Transaction;
import org.edu_sharing.elasticsearch.alfresco.client.Transactions;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchService;
import org.edu_sharing.elasticsearch.elasticsearch.client.Tx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public abstract class TransactionTrackerBase implements TransactionTrackerInterface{
    @Autowired
    protected AlfrescoWebscriptClient client;

    @Autowired
    protected ElasticsearchService elasticClient;

    @Autowired
    protected EduSharingClient eduSharingClient;

    @Value("${transactions.max:500}")
    int transactionsMax;

    Logger logger = LoggerFactory.getLogger(TransactionTrackerBase.class);

    //max value vor recursive track() calls, prevents stackoverflow error
    @Value("${stack.max:1000}")
    int maxStackSize;

    @Value("${threading.threadCount}")
    Integer threadCount;
    protected ForkJoinPool threadPool;

    @PostConstruct
    public void initBase() {
        threadPool = new ForkJoinPool(threadCount);
    }


    @Override
    public boolean track(){
        try {
            if(!elasticClient.isReady()){
                logger.info("waiting for ElasticsearchClient...");
                return false;
            }

            eduSharingClient.refreshValuespaceCache();
            Tx txn = elasticClient.getTransaction(getTransactionIndex());

            long lastTransactionId;
            if(txn != null){
                lastTransactionId = txn.getTxnId();
                logger.info("got last transaction from index txnId:" +lastTransactionId);
            }else{
                lastTransactionId = 0;
                logger.info("no transaction processed");
            }

            logger.info("starting lastTransactionId:" + lastTransactionId);

            //next tx id is last processed transactionId + 1
            long nextTransactionId = lastTransactionId + 1;
            Transactions transactions = client.getTransactions(nextTransactionId, nextTransactionId + transactionsMax, null, null, transactionsMax);

            long maxTrackerTxnId = getMaxTxnId(transactions);
            Long maxTxnId = transactions.getMaxTxnId();
            if(nextTransactionId >= maxTrackerTxnId || nextTransactionId >= maxTxnId){
                logger.info("Tracker "+ this.getClass().getSimpleName() +" is up to date. maxTrackerTxnId:"+ maxTrackerTxnId +" maxTxnId:" + maxTxnId +" lastTransactionId:" +lastTransactionId);
                return false;
            }

            if(transactions.getTransactions().isEmpty()){
                if(maxTrackerTxnId <= (lastTransactionId + transactionsMax)){
                    logger.info("index is up to date getMaxTxnId():"+ maxTrackerTxnId);
                    return false;
                }else{
                    logger.info("did not found new transactions in last transaction block min:" + lastTransactionId +" max:"+(lastTransactionId + transactionsMax)  );
                    commit(lastTransactionId + (long) transactionsMax);
                    return true;
                }
            }

            /**
             * get nodes
             */
            List<Long> transactionIds = new ArrayList<>();
            for(Transaction t : transactions.getTransactions()){
                transactionIds.add(t.getId());
            }
            logger.info("got "+transactionIds.size() +" transactions last:"+transactionIds.get(transactionIds.size() - 1));
            List<Node> nodes =  client.getNodes(transactionIds);
            logger.info("got "+nodes.size() +" nodes");

            eduSharingClient.refreshValuespaceCache();
            /**
             * index nodes
             */
            trackNodes(nodes);


            /**
             * remember prcessed transaction
             */
            Long last = transactionIds.get(transactionIds.size() - 1);
            commit(last);

            /**
             * log progress
             */
            DecimalFormat df = new DecimalFormat("0.00");
            logger.info("finished "+df.format(calcProgress(transactions,transactionIds))+"%, lastTransactionId:" +
                    " transactions:" + Arrays.toString(transactionIds.toArray()) +
                    " nodes:" + nodes.size() + "Stack size:" + Thread.currentThread().getStackTrace().length);

            if(Thread.currentThread().getStackTrace().length >= maxStackSize){
                logger.info("reached max stack size of: " +maxStackSize + "<=" +Thread.currentThread().getStackTrace().length);
                return false;
            }

            return true;

        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return false;
        }

    }

    private Double calcProgress(Transactions transactions, List<Long> transactionIds){
        Long last = transactionIds.get(transactionIds.size() - 1);
        return (double) last / (double)getMaxTxnId(transactions) * 100.0d;
    }

    void commit(long txId) throws IOException{
        logger.info("safe transactionId " + txId);
        elasticClient.setTransaction(getTransactionIndex(),0L,txId);
    }

    public abstract void trackNodes(List<Node> nodes) throws IOException;

    public String getTransactionIndex(){
        return ElasticsearchService.INDEX_TRANSACTIONS;
    }

    public long getMaxTxnId(Transactions transactions){
        return transactions.getMaxTxnId();
    }

}
