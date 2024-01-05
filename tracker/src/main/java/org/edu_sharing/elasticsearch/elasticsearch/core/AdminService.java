package org.edu_sharing.elasticsearch.elasticsearch.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminService implements ApplicationContextAware {  //, SmartInitializingSingleton {
    private final ElasticsearchClient client;

    private final Collection<IndexConfiguration> indexConfigurations;

    @Setter
    private ApplicationContext applicationContext;


    public boolean createIndex(Collection<IndexConfiguration> indexConfigurations) throws IOException {
        boolean createdAnyIndex = false;
        for (IndexConfiguration indexConfiguration : indexConfigurations) {
            try {
                if (!client.indices().exists(req -> req.index(indexConfiguration.getIndex())).value()) {
                    client.indices().create(indexConfiguration.getCreateIndexRequest());
                    createdAnyIndex = true;
                }
            } catch (IOException ex) {
                log.error("create index {} failed with {}", indexConfiguration.getIndex(), ex.getMessage(), ex);
                throw ex;
            }
        }
        return createdAnyIndex;
    }

    public boolean createIndex(IndexConfiguration indexConfiguration, IndexConfiguration... indexConfigurations) throws IOException {
        List<IndexConfiguration> list = new ArrayList<>();
        list.add(indexConfiguration);
        list.addAll(List.of(indexConfigurations));
        return createIndex(list);
    }


    public void deleteIndex(IndexConfiguration indexConfiguration, IndexConfiguration... indexConfigurations) throws IOException {
        List<String> indices = new ArrayList<>();
        indices.add(indexConfiguration.getIndex());
        indices.addAll(Arrays.stream(indexConfigurations).map(IndexConfiguration::getIndex).collect(Collectors.toList()));

        try {
            client.indices().delete(req -> req.index(indices));
        } catch (IOException ex) {
            log.error("delete index [{}] failed with {}", String.join(", ", indices), ex.getMessage(), ex);
            throw ex;
        }
    }

    @PostConstruct
    public void init() {
        try {
            createIndex(indexConfigurations);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//    @SneakyThrows
//    @Override
//    public void afterSingletonsInstantiated() {
//        Collection<IndexConfiguration> indexConfigurations = applicationContext.getBeansOfType(IndexConfiguration.class).values();
//        try {
//            createIndex(indexConfigurations);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
