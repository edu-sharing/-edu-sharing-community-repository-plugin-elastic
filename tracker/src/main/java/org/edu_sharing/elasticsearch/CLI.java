package org.edu_sharing.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.elasticsearch.elasticsearch.core.IndexConfiguration;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CLI implements Callable<Integer>
{
    private final ElasticsearchClient elasticsearchClient;
    private final List<IndexConfiguration> indexConfigurationList;

    @Option(names = "--drop-index", description = "CAREFUL! Drops the whole current index and forces a full re-index (this can take several hours)")
    public Boolean clearIndex;

    @Override
    public Integer call() throws Exception {
        if(Boolean.TRUE.equals(clearIndex)) {
            List<String> indices = indexConfigurationList.stream().map(IndexConfiguration::getIndex).collect(Collectors.toList());
            elasticsearchClient.indices().delete(req -> req.index(indices));
            log.info("Dropped index {}", String.join(", ", indices));
            return 0;
        }

        return -1;
    }

}
