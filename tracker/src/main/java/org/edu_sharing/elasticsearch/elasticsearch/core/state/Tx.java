package org.edu_sharing.elasticsearch.elasticsearch.core.state;

import lombok.Data;

@Data
public class Tx {
    private long txnId;
    private long txnCommitTime;

    // Required for deserialization
    public Tx() {

    }
    public Tx(long txnId, long txnCommitTime) {
        this.txnId = txnId;
        this.txnCommitTime = txnCommitTime;
    }

}
