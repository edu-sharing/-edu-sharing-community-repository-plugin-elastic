package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.AlfrescoWebscriptClient;
import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.Transaction;
import org.edu_sharing.elasticsearch.alfresco.client.Transactions;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.Tx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class TransactionTrackerBase implements TransactionTrackerInterface{
    @Autowired
    protected AlfrescoWebscriptClient client;

    @Autowired
    protected ElasticsearchClient elasticClient;

    @Value("${transactions.max:500}")
    int transactionsMax;

    Logger logger = LoggerFactory.getLogger(TransactionTrackerBase.class);




    @Override
    public boolean track(){
        try {
            Tx txn = elasticClient.getTransaction();

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

            if(transactions.getTransactions().size() == 0){
                if(transactions.getMaxTxnId() <= (lastTransactionId + transactionsMax)){
                    logger.info("index is up to date transactions.getMaxTxnId():"+transactions.getMaxTxnId());
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
            return true;

        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
            return false;
        }

    }

    private Double calcProgress(Transactions transactions, List<Long> transactionIds){
        Long last = transactionIds.get(transactionIds.size() - 1);
        return  (transactionIds != null && transactionIds.size() > 0) ? new Double(((double) last / (double)transactions.getMaxTxnId()) * 100.0)  : 0.0;
    }

    void commit(long txId) throws IOException{
        logger.info("safe transactionId " + txId);
        elasticClient.setTransaction(0L,txId);
    }

    public abstract void trackNodes(List<Node> nodes);

}
