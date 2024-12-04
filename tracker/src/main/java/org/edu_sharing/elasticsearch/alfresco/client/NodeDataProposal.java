package org.edu_sharing.elasticsearch.alfresco.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class NodeDataProposal extends NodeData{
    NodeData collection;
    NodeData original;

    public NodeData getCollection() {
        return collection;
    }

    public void setCollection(NodeData collection) {
        this.collection = collection;
    }

    public NodeData getOriginal() {
        return original;
    }

    public void setOriginal(NodeData original) {
        this.original = original;
    }
}
