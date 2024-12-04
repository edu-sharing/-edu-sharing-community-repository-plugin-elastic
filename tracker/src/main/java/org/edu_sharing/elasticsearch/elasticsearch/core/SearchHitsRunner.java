package org.edu_sharing.elasticsearch.elasticsearch.core;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

public final class SearchHitsRunner {

    private static final Logger logger = LoggerFactory.getLogger(SearchHitsRunner.class);

    private final WorkspaceService workspaceService;
    public SearchHitsRunner(WorkspaceService workspaceService){
        this.workspaceService = workspaceService;
    }

    public void run(Query query, Consumer<Hit<Map>> hitConsumer)throws IOException {
        this.run(query,5, hitConsumer);
    }

    public void run(Query query, int pageSize, Consumer<Hit<Map>> hitConsumer)throws IOException {
        this.run(query,pageSize, null, hitConsumer);
    }

    public void run(Query query, int pageSize, Integer maxResultsSize,  Consumer<Hit<Map>> hitConsumer)throws IOException {
        int page = 0;
        HitsMetadata<Map> searchHits = null;
        do{
            if(searchHits != null){
                page+=pageSize;
            }
            searchHits = workspaceService.search(query, page, pageSize);
            if(maxResultsSize != null && searchHits.total().value() > maxResultsSize){
                logger.warn("max result size has been reached: found {} of {} allowed", searchHits.total().value(), maxResultsSize);
                return;
            }
            logger.debug("result is smaller than limit, will continue: found {} of {} allowed", searchHits.total().value(), maxResultsSize);

            for(Hit<Map> searchHit : searchHits.hits()){
                hitConsumer.accept(searchHit);
            }

        }while(searchHits.total() != null && searchHits.total().value() > page);
    }
}
