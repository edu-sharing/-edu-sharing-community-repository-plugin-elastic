package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


@Component
@ConditionalOnProperty(prefix = "transaction", name="tracker", havingValue = "fix-missing")
public class FixMissingTracker extends TransactionTrackerBase{

    Logger logger =  LoggerFactory.getLogger(FixMissingTracker.class);


    @Override
    public void trackNodes(List<Node> nodes) throws IOException {
        logger.info("tracking the following number of nodes:" + nodes.size());
    }
}
