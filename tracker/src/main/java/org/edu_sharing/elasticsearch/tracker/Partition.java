package org.edu_sharing.elasticsearch.tracker;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Partition <T>{

    Collection<List<T>> getPartitions(List<T> list, int partitionSize){
        final AtomicInteger counter = new AtomicInteger(0);
        return list.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / partitionSize))
                .values();
    }
}
