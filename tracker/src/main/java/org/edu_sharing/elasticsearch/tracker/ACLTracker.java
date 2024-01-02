package org.edu_sharing.elasticsearch.tracker;

import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.elasticsearch.client.ACLChangeSet;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class ACLTracker {

    private final AlfrescoWebscriptClient client;
    private final ElasticsearchService elasticClient;

    @Value("${allowed.types}")
    String allowedTypes;

    long lastFromCommitTime = -1;
    long lastACLChangeSetId = -1;

    final static int maxResults = 500;

    @Value("${tracker.timestep:36000000}")
    int nextTimeStep;

    Logger logger = LoggerFactory.getLogger(ACLTracker.class);

    public ACLTracker(AlfrescoWebscriptClient client, ElasticsearchService elasticClient) {
        this.client = client;
        this.elasticClient = elasticClient;
    }

    @PostConstruct
    public void init() throws IOException {
        ACLChangeSet aclChangeSet;
        try {
            aclChangeSet = elasticClient.getACLChangeSet();
            if (aclChangeSet != null) {
                lastFromCommitTime = aclChangeSet.getAclChangeSetCommitTime();
                lastACLChangeSetId = aclChangeSet.getAclChangeSetId();
                logger.info("got last aclChangeSet from index aclCommitTime:" + aclChangeSet.getAclChangeSetCommitTime() + " aclId" + aclChangeSet.getAclChangeSetId());
            }
        } catch (IOException e) {
            logger.error("problems reaching elastic search server");
            throw e;
        }
    }

    public boolean track() {
        if(!elasticClient.isReady()) {
            logger.info("waiting for ElasticsearchClient...");
            return false;
        }

        logger.info("starting lastACLChangeSetId:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime + " " + new Date(lastFromCommitTime));


        AclChangeSets aclChangeSets = (lastACLChangeSetId < 1)
                ? client.getAclChangeSets(0L, 500L, 1)
                : client.getAclChangeSets(lastACLChangeSetId, lastACLChangeSetId + ACLTracker.maxResults, ACLTracker.maxResults);


        //initialize
        if (lastACLChangeSetId < 1) lastACLChangeSetId = aclChangeSets.getAclChangeSets().get(0).getId();

        //step forward
        if (aclChangeSets.getMaxChangeSetId() > (lastACLChangeSetId + ACLTracker.maxResults)) {
            lastACLChangeSetId += ACLTracker.maxResults;
        } else {
            lastACLChangeSetId = aclChangeSets.getMaxChangeSetId();
        }


        if (aclChangeSets.getAclChangeSets().isEmpty()) {

            if (aclChangeSets.getMaxChangeSetId() <= lastACLChangeSetId) {
                logger.info("index is up to date:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime);
                //+1 to prevent repeating the last transaction over and over
                //not longer necessary when we remember last transaction id in idx
                this.lastFromCommitTime = aclChangeSets.getMaxChangeSetId() + 1;
            } else {
                logger.info("did not found new aclchangesets in last aclchangeset block from:" + (lastACLChangeSetId - ACLTracker.maxResults) + " to:" + lastACLChangeSetId + " MaxChangeSetId:" +aclChangeSets.getMaxChangeSetId());
            }
            return false;
        }

        AclChangeSet last = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1);

        try {
            ACLChangeSet aclChangeSet = elasticClient.getACLChangeSet();
            if(aclChangeSet != null && (aclChangeSet.getAclChangeSetId() == aclChangeSets.getMaxChangeSetId())){
                logger.info("nothing to do.");
                return false;
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            return false;
        }


        if (lastFromCommitTime < 1) {
            this.lastFromCommitTime = last.getCommitTimeMs();
        }


        GetAclsParam param = new GetAclsParam();
        for (AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets()) {
            param.getAclChangeSetIds().add(aclChangeSet.getId());
        }


        Acls acls = client.getAcls(param);

        GetPermissionsParam grp = new GetPermissionsParam();
        Map<Long, Acl> aclIdMap = acls.getAcls().stream()
                .collect(Collectors.toMap(Acl::getId, accessControlList -> accessControlList));

        grp.setAclIds(new ArrayList<>(aclIdMap.keySet()));
        ReadersACL readers = client.getReader(grp);
        Map<Long, Reader> readersMap = readers.getAclsReaders().stream()
                .collect(Collectors.toMap(Reader::getAclId, readersList -> readersList));

        logger.debug(grp.getAclIds().toString());
        AccessControlLists accessControlLists = client.getAccessControlLists(grp);
        Map<Long, AccessControlList> accessControlListMap = accessControlLists.getAccessControlLists().stream()
                .collect(Collectors.toMap(AccessControlList::getAclId, accessControlList -> accessControlList));

        try {
            for (Acl acl : acls.getAcls()) {

                Reader reader = readersMap.get(acl.getId());
                if (reader.getAclId() != acl.getId()) {
                    logger.error("reader aclid:" + reader.getAclId() + " does not match " + acl.getId());
                    continue;
                }

                List<String> alfReader = reader.getReaders();
                Collections.sort(alfReader);
                /**
                 *  alfresco permissions
                 */
                Map<String, List<String>> permissionsAlf = new HashMap<>();
                for (AccessControlEntry ace : accessControlListMap.get(acl.getId()).getAces()) {
                    List<String> authorities = permissionsAlf.get(ace.getPermission());
                    if (authorities == null) {
                        authorities = new ArrayList<>();
                    }
                    if (!authorities.contains(ace.getAuthority())) {
                        authorities.add(ace.getAuthority());
                    }
                    Collections.sort(authorities);
                    permissionsAlf.put(ace.getPermission(), authorities);
                }
                if (!alfReader.isEmpty()) {
                    permissionsAlf.put("read", alfReader);
                }
                //sort alf map keys:
                permissionsAlf = new TreeMap<>(permissionsAlf);
                elasticClient.updateNodesWithAcl(acl.getId(),permissionsAlf);
            }
            long lastAclChangesetid = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1).getId();
            elasticClient.setACLChangeSet(lastFromCommitTime, lastAclChangesetid);
        }catch(IOException e){
            logger.error("elastic search server not reachable", e);
        }

        double percentage = ((double) aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1).getId() - 1) / (double) aclChangeSets.getMaxChangeSetId() * 100.0d;
        DecimalFormat df = new DecimalFormat("0.00");
        logger.info("finished "+df.format(percentage)+"% lastACLChangeSetId:" + last.getId());
        return true;
    }
}
