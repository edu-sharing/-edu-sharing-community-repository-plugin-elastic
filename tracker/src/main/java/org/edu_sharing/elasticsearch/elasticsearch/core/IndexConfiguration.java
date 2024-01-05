package org.edu_sharing.elasticsearch.elasticsearch.core;

import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.util.ObjectBuilder;
import lombok.Getter;

import java.util.function.Function;

@Getter
public class IndexConfiguration {
    private final String index;
    private final CreateIndexRequest createIndexRequest;

    public IndexConfiguration(CreateIndexRequest createIndexRequest) {
        this.index = createIndexRequest.index();
        this.createIndexRequest = createIndexRequest;
    }

    public IndexConfiguration(Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>> fn) {
        this(CreateIndexRequest.of(fn));
    }


}
