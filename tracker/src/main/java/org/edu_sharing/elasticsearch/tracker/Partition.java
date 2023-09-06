package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.Node;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Partition <T>{

    Collection<List<T>> getPartitions(List<T> list, int partitionSize){
        final AtomicInteger counter = new AtomicInteger(0);
        Collection<List<T>> partitions = list.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / partitionSize))
                .values();
        return partitions;
    }
}
