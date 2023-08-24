package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.NodeMetadata;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.Tx;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.elasticsearch.action.get.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Component
@ConditionalOnProperty(prefix = "transaction", name="tracker", havingValue = "fix-missing")
public class FixMissingTracker extends TransactionTracker{

    public static String INDEX_TRANSACTIONS = "transactions_missing";

    Logger logger =  LoggerFactory.getLogger(FixMissingTracker.class);


    /**
     * create nodes missed in index
     */
    @Value("${fixmissing.repair:false}")
    boolean repair;

    File tempFile;


    @PostConstruct
    void createTempFile() throws IOException {
        String tmpdir = System.getProperty("java.io.tmpdir");
        String tmpFileName = "fixmissing_md_fetch_error.log";
        tempFile = new File(tmpdir + "/"+tmpFileName);

        Tx transaction = elasticClient.getTransaction(getTransactionIndex());
        if(transaction == null && tempFile.exists()){
            logger.info("clearing error log");
            Files.delete(tempFile.toPath());
        }

        if(!tempFile.exists()){
            Files.createFile(tempFile.toPath());
        }
    }


    @Override
    public void trackNodes(List<Node> nodes) throws IOException {
        logger.info("tracking the following number of nodes:" + nodes.size());

        //filter stores
        nodes = nodes
                .stream()
                .filter(n -> indexStoreRefs.contains(Tools.getStoreRef(n.getNodeRef())))
                .collect(Collectors.toList());

        //filter deletes
        nodes = nodes
                .stream()
                .filter(n -> !n.getStatus().equals("d"))
                .collect(Collectors.toList());


        List<NodeMetadata> nodeData = client.getNodeMetadataAsSingleOnExeption(nodes);
        for(Node node : nodes){
            boolean isPresent = nodeData.stream().filter(n ->  n.getId() == node.getId()).findFirst().isPresent();
            if(!isPresent){
                logNodeProblem("Problem fetching NodeMetadata for",node);
                logUnresolveableNode(new Long(node.getId()).toString());
            }
        }

        for(NodeMetadata nodeMetadata : nodeData){
            if(isAllowedType(nodeMetadata)) {
                long startMs = System.currentTimeMillis();
                GetResponse resp = elasticClient.get(ElasticsearchClient.INDEX_WORKSPACE, new Long(nodeMetadata.getId()).toString());
                logger.info("get took:" + (System.currentTimeMillis() - startMs) + "ms");
                if (!resp.isExists()) {
                    logNodeProblem("node does not exist in elastic id:", nodeMetadata);
                    if(repair){
                        Node node = nodes.stream().filter(n -> n.getId() == nodeMetadata.getId()).findFirst().get();
                        indexNodes(Arrays.asList(node));
                    }
                }
            }
        }
    }

    private void logNodeProblem(String message, Node node){
        logger.error(message + " " + node.getId() +" nodeRef:"+node.getNodeRef() +" s:"+node.getStatus() +" txId:"+node.getTxnId());
    }

    private void logNodeProblem(String message, NodeMetadata node){
        logger.error(message + " " + node.getId() +" nodeRef:"+node.getNodeRef() + " type:" + node.getType() +" txId:"+node.getTxnId());
    }

    @Override
    public String getTransactionIndex() {
        return INDEX_TRANSACTIONS;
    }

    private void logUnresolveableNode(String dbid) throws IOException{
        Files.write(tempFile.toPath(),dbid.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    }
}
