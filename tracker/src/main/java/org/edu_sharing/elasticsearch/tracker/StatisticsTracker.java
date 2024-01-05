package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.edu_sharing.client.NodeStatistic;
import org.edu_sharing.elasticsearch.elasticsearch.core.WorkspaceService;
import org.edu_sharing.elasticsearch.elasticsearch.core.state.StatisticTimestamp;
import org.edu_sharing.elasticsearch.elasticsearch.core.StatusIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class StatisticsTracker {

    @Value("${statistic.historyInDays}")
    long historyInDays;

    private final WorkspaceService elasticService;
    private final EduSharingClient eduSharingClient;
    private final StatusIndexService<StatisticTimestamp> statisticTimestampStateService;




    Logger logger = LoggerFactory.getLogger(StatisticsTracker.class);

    Map<Integer, List<Map.Entry<String, List<NodeStatistic>>>> currentChunks  = new HashMap<>();
    int chunkSize = 1000;
    long trackTs = -1;
    long trackTsTo = -1;
    boolean allNodesInIndex = true;

    public StatisticsTracker(WorkspaceService elasticService, EduSharingClient eduSharingClient, StatusIndexService<StatisticTimestamp> statisticTimestampStateService) {
        this.elasticService = elasticService;
        this.eduSharingClient = eduSharingClient;
        this.statisticTimestampStateService = statisticTimestampStateService;
    }

    public void track(){
        try {
            if(currentChunks.isEmpty()) {
                allNodesInIndex = true;

                trackTs = getTodayMidnight();
                long trackFromTime = trackTs - (historyInDays * 24L * 60L * 60L * 1000L);
                StatisticTimestamp statisticTimestamp = statisticTimestampStateService.getState();
                if (statisticTimestamp != null) {
                    trackFromTime = statisticTimestamp.getStatisticTimestamp();
                    logger.info("starting from last run " + new Date(trackFromTime));
                } else {
                    logger.info("starting from history " + new Date(trackFromTime));
                }

                trackTsTo = System.currentTimeMillis();
                Map<String, List<NodeStatistic>> nodeStatistics = new HashMap<>();
                List<String> statistics = eduSharingClient.getStatisticsNodeIds(trackFromTime);
                logger.info("found " + statistics.size() + " statistic changes");

                for(String nodeId : statistics){
                    List<NodeStatistic> statisticsForNode = eduSharingClient.getStatisticsForNode(nodeId, trackFromTime);
                    nodeStatistics.put(nodeId,statisticsForNode);
                }
                
                AtomicInteger counter = new AtomicInteger();

                currentChunks = nodeStatistics.entrySet().stream().collect(Collectors.groupingBy(e -> counter.getAndIncrement() / chunkSize));
                logger.info("splitted into "+currentChunks.size() +" chunks");
            }

            List<Integer> successfullChunks = new ArrayList<>();
            for(Map.Entry<Integer, List<Map.Entry<String, List<NodeStatistic>>>> entry : currentChunks.entrySet()){
                logger.info("current chunk:"+ entry.getKey() +" size: "+entry.getValue().size() +" all chunks:"+currentChunks.size());
                Map<String,List<NodeStatistic>> nodeStatistics = new HashMap<>();
                for(Map.Entry<String,List<NodeStatistic>> e : entry.getValue()){
                    nodeStatistics.put(e.getKey(),e.getValue());
                }
                try{
                    allNodesInIndex = allNodesInIndex && elasticService.updateNodeStatistics(nodeStatistics);
                    elasticService.cleanUpNodeStatistics(new ArrayList<>(nodeStatistics.keySet()));
                    successfullChunks.add(entry.getKey());
                }catch (IOException e){
                    logger.error("problems reaching elastic search server",e);
                }
            }
            successfullChunks.forEach(c -> currentChunks.remove(c));

            if(currentChunks.isEmpty()) {
                logger.info("finished statistics until:" + new Date(trackTsTo));
                statisticTimestampStateService.setState(new StatisticTimestamp(allNodesInIndex, trackTsTo));
            }
            elasticService.refreshWorkspace();

        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }

    private long getTodayMidnight(){
        Calendar date = Calendar.getInstance();
// reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTime().getTime();
    }
}
