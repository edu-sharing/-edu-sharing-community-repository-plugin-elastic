package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.alfresco.client.NodeData;
import org.edu_sharing.elasticsearch.alfresco.client.NodeMetadata;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.edu_sharing.client.NodeStatistic;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(prefix = "transaction", name="tracker", havingValue = "default", matchIfMissing = true)
public class TransactionTracker extends TransactionTrackerBase{


    @Value("${allowed.types}")
    String allowedTypes;

    List<String> subTypes = Arrays.asList(new String[]{"ccm:io","ccm:rating","ccm:comment","ccm:usage","ccm:collection_proposal"});

    @Value("${index.storerefs}")
    List<String> indexStoreRefs;


    Logger logger = LoggerFactory.getLogger(TransactionTracker.class);

    @Value("${statistic.historyInDays}")
    long historyInDays;


    @Override
    public void trackNodes(List<Node> nodes) throws IOException{

        //filter stores
        nodes = nodes
                .stream()
                .filter(n -> indexStoreRefs.contains(Tools.getStoreRef(n.getNodeRef())))
                .collect(Collectors.toList());

        if(nodes.size() == 0){
            return;
        }

        /**
         * collect deletes
         */
        List<Node> toDelete = new ArrayList<Node>();
        for(Node node : nodes){
            if(node.getStatus().equals("d")) {
                toDelete.add(node);
            }
        }

        //filter deletes
        nodes = nodes
                .stream()
                .filter(n -> !n.getStatus().equals("d"))
                .collect(Collectors.toList());


        elasticClient.beforeDeleteCleanupCollectionReplicas(toDelete);
        elasticClient.delete(toDelete);

        /**
         * index nodes
         */
        //some transactions can have a lot of Nodes which can cause trouble on alfresco so use partitioning
        final AtomicInteger counter = new AtomicInteger(0);
        final int size = 100;
        Collection<List<Node>> partitions = nodes.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size))
                .values();
        int pIdx = 0;
        for(List<Node> partition :  partitions){
            logger.info("indexNodes partition " +pIdx);
            indexNodes(partition);
            pIdx++;
        }
    }

    public void indexNodes(List<Node> nodes) throws IOException{
        logger.info("getNodeMetadata start " + nodes.size());
        List<NodeMetadata> nodeData = client.getNodeMetadata(nodes);
        logger.info("getNodeMetadata done " +nodeData.size());
        indexNodesMetadata(nodeData);
    }
    public void indexNodesMetadata(List<NodeMetadata> nodeData) throws IOException{

        List<NodeMetadata> toIndexUsagesProposalsMd = nodeData
                .stream()
                .filter(n -> "ccm:usage".equals(n.getType())
                        || "ccm:collection_proposal".equals(n.getType()))
                .collect(Collectors.toList());

        List<NodeMetadata> toIndexMd = new ArrayList<>();
        List<Node> ioSubobjectChange = new ArrayList<>();
        for(NodeMetadata data : nodeData){

            //force reindex of parent io to get subobjects
            if(subTypes.contains(data.getType())
                    && (!data.getType().equals("ccm:io") || data.getAspects().contains("ccm:io_childobject"))
                    && CCConstants.STORE_WORKSPACES_SPACES.equals(Tools.getStoreRef(data.getNodeRef()))){

                String[] splitted = data.getPaths().get(0).getApath().split("/");
                String parentId = splitted[splitted.length -1];
                Serializable value = elasticClient.getProperty(CCConstants.STORE_WORKSPACES_SPACES+"/"+parentId,"dbid");
                if(value != null){
                    Long parentDbid = ((Number)value).longValue();
                    logger.info("FOUND PARENT IO WITH "+ parentDbid);
                    //check if exists in list
                    if(!nodeData.stream().anyMatch(n -> n.getId() == parentDbid)){
                        Node n = new Node();
                        n.setId(parentDbid);
                        ioSubobjectChange.add(n);
                    }
                }//else io does not exist in index
            }

            if(allowedTypes != null && !allowedTypes.trim().equals("")){
                String[] allowedTypesArray = allowedTypes.split(",");
                String type = data.getType();

                if(!Arrays.asList(allowedTypesArray).contains(type)){
                    logger.debug("ignoring type:" + type);
                    continue;
                }
            }
            toIndexMd.add(data);
        }

        if(ioSubobjectChange.size() > 0){
            toIndexMd.addAll(client.getNodeMetadata(ioSubobjectChange));
        }

        List<NodeData> toIndex = client.getNodeData(toIndexMd);
        for(NodeData data: toIndex) {
            if(data.getNodeMetadata().getNodeRef().startsWith(CCConstants.ARCHIVE_STOREREF) ){
                //skipping preview and valuespace translation for archived nodes
                continue;
            }
            threadPool.execute(() -> {
                eduSharingClient.addPreview(data);
                eduSharingClient.translateValuespaceProps(data);
            });
        }
        if(!threadPool.awaitQuiescence(10, TimeUnit.MINUTES)){
            logger.error("Fatal error while processing nodes: timeout of preview and transform processing");
            logger.error(nodeData.stream().map(NodeMetadata::getNodeRef).collect(Collectors.joining(", ")));
        }

        logger.info("final usable: " + toIndexUsagesProposalsMd.size() + " " + toIndex.size());

        final AtomicInteger counter = new AtomicInteger(0);
        final int size = 50;
        final Collection<List<NodeData>> partitioned = toIndex.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size))
                .values();
        for(List<NodeData> p : partitioned){
            elasticClient.index(p);
        }
        for(NodeData nodeDataStat : toIndex){
            if(!"ccm:io".equals(nodeDataStat.getNodeMetadata().getType())
                    || !Tools.getProtocol(nodeDataStat.getNodeMetadata().getNodeRef()).equals("workspace")){
                continue;
            }
            long trackTs = System.currentTimeMillis();
            long trackFromTime = trackTs - (historyInDays * 24L * 60L * 60L * 1000L);
            String nodeId = Tools.getUUID(nodeDataStat.getNodeMetadata().getNodeRef());
            List<NodeStatistic> statisticsForNode = eduSharingClient.getStatisticsForNode(nodeId, trackFromTime);
            Map<String,List<NodeStatistic>> updateNodeStatistics = new HashMap<>();
            updateNodeStatistics.put(nodeId,statisticsForNode);
            elasticClient.updateNodeStatistics(updateNodeStatistics);
            //we don't need cleanup cause former elasticClient.index(..) call removes all statistic data
            //elasticClient.cleanUpNodeStatistics(nodeDataStat);
        }

        /**
         * refresh index so that collections will be found by cacheCollections process
         */
        elasticClient.refresh(ElasticsearchClient.INDEX_WORKSPACE);
        for(NodeMetadata usage : toIndexUsagesProposalsMd) elasticClient.indexCollections(usage);
    }

    public boolean isAllowedType(NodeMetadata nodeMetadata){
        if(allowedTypes != null && !allowedTypes.trim().equals("")){
            String[] allowedTypesArray = allowedTypes.split(",");
            String type = nodeMetadata.getType();

            if(!Arrays.asList(allowedTypesArray).contains(type)){
                return false;
            }
        }
        return true;
    }
}
