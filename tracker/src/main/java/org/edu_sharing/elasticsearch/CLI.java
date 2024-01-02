package org.edu_sharing.elasticsearch;

import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(helpCommand = false, mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CLI implements Callable<Integer>
{
    @Autowired
    ElasticsearchService elasticsearchClient;
    @CommandLine.Option(names = "--drop-index", description = "CAREFUL! Drops the whole current index and forces a full re-index (this can take several hours)")
    Boolean clearIndex;
    @Override
    public Integer call() throws Exception {
        if(Boolean.TRUE.equals(clearIndex)) {
            elasticsearchClient.deleteIndex(ElasticsearchService.INDEX_TRANSACTIONS);
            System.out.println("Droped index " + ElasticsearchService.INDEX_TRANSACTIONS);
            elasticsearchClient.deleteIndex(ElasticsearchService.INDEX_WORKSPACE);
            System.out.println("Droped index " + ElasticsearchService.INDEX_WORKSPACE);
            return 0;
        }
        // nothing to do, we use the mixinStandardHelpOptions
        return -1;
    }

}
