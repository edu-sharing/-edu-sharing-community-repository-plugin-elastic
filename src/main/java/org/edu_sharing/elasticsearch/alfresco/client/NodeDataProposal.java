package org.edu_sharing.elasticsearch.alfresco.client;

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
