package org.edu_sharing.elasticsearch.elasticsearch.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.elasticsearch.elasticsearch.core.state.ACLChangeSet;
import org.edu_sharing.elasticsearch.elasticsearch.core.state.StatisticTimestamp;
import org.edu_sharing.elasticsearch.elasticsearch.core.state.Tx;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StatusIndexServiceFactory {

    private final ElasticsearchClient client;

    public StatusIndexService<Tx> createTransactionStateService(String index){
        return new StatusIndexService<>(index, client, "1", Tx.class);
    }

    public StatusIndexService<ACLChangeSet> createAclStateService(String index){
        return new StatusIndexService<>(index, client, "2", ACLChangeSet.class);
    }

    public StatusIndexService<StatisticTimestamp> createStatisticTimestampStateService(String index){
        return new StatusIndexService<>(index, client, "3", StatisticTimestamp.class);
    }
}
