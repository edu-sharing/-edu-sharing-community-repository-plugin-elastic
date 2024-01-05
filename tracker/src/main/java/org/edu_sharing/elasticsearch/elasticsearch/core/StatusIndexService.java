package org.edu_sharing.elasticsearch.elasticsearch.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.ShardFailure;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class StatusIndexService<TDATA> {

    private final String index;
    private final ElasticsearchClient client;

    private final String id;
    private final Class<TDATA>  statusType;


    public TDATA getState() throws IOException {
        return client.get(req -> req
                                .index(index)
                                .id(id),
                        statusType)
                .source();
    }

    public void setState(TDATA state) throws IOException {
        IndexResponse indexResponse = client.index(req -> req
                .index(index)
                .id(id)
                .document(state));


        if (indexResponse.result() == Result.Created) {
            log.debug("created node in elastic: {}", state);
        } else if (indexResponse.result() == Result.Updated) {
            log.debug("updated node in elastic: {}", state);
        }

        ShardStatistics shardInfo = indexResponse.shards();
        if (shardInfo.total().longValue() != shardInfo.successful().longValue()) {
            log.debug("shardInfo.total().longValue() {} != shardInfo.successful().longValue(): {}", shardInfo.total().longValue(), shardInfo.successful().longValue());
        }

        if (shardInfo.failed().longValue() > 0) {
            for (ShardFailure failure : shardInfo.failures()) {
                String reason = failure.reason().reason();
                log.error("{} reason: {}", failure.node(), reason);
            }
        }
    }
}
