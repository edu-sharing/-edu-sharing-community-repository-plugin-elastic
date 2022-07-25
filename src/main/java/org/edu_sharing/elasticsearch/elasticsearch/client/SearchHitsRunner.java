package org.edu_sharing.elasticsearch.elasticsearch.client;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class SearchHitsRunner {

    private static final Logger logger = LoggerFactory.getLogger(SearchHitsRunner.class);

    protected ElasticsearchClient elasticClient;
    public SearchHitsRunner(ElasticsearchClient elasticClient){
        this.elasticClient = elasticClient;
    }

    public void run(QueryBuilder queryBuilder)throws IOException {
        this.run(queryBuilder,5);
    }

    public void run(QueryBuilder queryBuilder, int pageSize)throws IOException {
        this.run(queryBuilder,pageSize, null);
    }

    public void run(QueryBuilder queryBuilder, int pageSize, Integer maxResultsSize)throws IOException {

        int page = 0;
        SearchHits searchHits = null;
        do{
            if(searchHits != null){
                page+=pageSize;
            }
            searchHits = elasticClient.search(ElasticsearchClient.INDEX_WORKSPACE, queryBuilder, page, pageSize);
            if(maxResultsSize != null && searchHits.getTotalHits().value > maxResultsSize){
                logger.warn("max result size has been reached: found {} of {} allowed", searchHits.getTotalHits().value, maxResultsSize);
                return;
            }
            for(SearchHit searchHit : searchHits.getHits()){
                execute(searchHit);
            }

        }while(searchHits.getTotalHits().value > page);
    }

    public abstract void execute(SearchHit hit) throws IOException;
}
