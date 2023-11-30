package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.Node;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Partition <T>{

    public Collection<List<T>> getPartitions(Collection<T> collection, int partitionSize){
        return getPartitionInternal(collection,partitionSize).values();
    }

    private Map<Integer, List<T>> getPartitionInternal(Collection<T> collection, int partitionSize){
        final AtomicInteger counter = new AtomicInteger(0);
        Map<Integer, List<T>> collect = collection.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / partitionSize));
        return collect;
    }
}
