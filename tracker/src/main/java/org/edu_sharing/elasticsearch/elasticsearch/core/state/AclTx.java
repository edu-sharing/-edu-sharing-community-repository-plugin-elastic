package org.edu_sharing.elasticsearch.elasticsearch.core.state;

import lombok.Data;

@Data
public class AclTx {

    private long aclChangeSetId;
    private long aclChangeSetCommitTime;

    // Required for deserialization
    public AclTx() {

    }

    public AclTx(long aclChangeSetId, long aclChangeSetCommitTime) {
        this.aclChangeSetId = aclChangeSetId;
        this.aclChangeSetCommitTime = aclChangeSetCommitTime;
    }

}
