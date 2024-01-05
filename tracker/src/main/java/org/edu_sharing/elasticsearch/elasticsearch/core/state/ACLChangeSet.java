package org.edu_sharing.elasticsearch.elasticsearch.core.state;

import lombok.Data;

@Data
public class ACLChangeSet {

    private long aclChangeSetId;
    private long aclChangeSetCommitTime;

    // Required for deserialization
    public ACLChangeSet() {

    }

    public ACLChangeSet(long aclChangeSetId, long aclChangeSetCommitTime) {
        this.aclChangeSetId = aclChangeSetId;
        this.aclChangeSetCommitTime = aclChangeSetCommitTime;
    }

}
