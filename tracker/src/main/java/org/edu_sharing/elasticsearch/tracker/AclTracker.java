package org.edu_sharing.elasticsearch.tracker;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.elasticsearch.core.StatusIndexService;
import org.edu_sharing.elasticsearch.elasticsearch.core.WorkspaceService;
import org.edu_sharing.elasticsearch.elasticsearch.core.state.AclTx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class AclTracker {

    private final AlfrescoWebscriptClient alfClient;
    private final WorkspaceService workspaceService;

    @Value("${allowed.types}")
    String allowedTypes;

    final static int maxResults = 500;

    @Value("${tracker.timestep:36000000}")
    int nextTimeStep;

    Logger logger = LoggerFactory.getLogger(AclTracker.class);
    private final StatusIndexService<AclTx> aclStateService;


//    @PostConstruct
//    public void init() throws IOException {
//        ACLChangeSet aclChangeSet;
//        try {
//            aclChangeSet = aclStateService.getState();
//            if (aclChangeSet != null) {
//                lastFromCommitTime = aclChangeSet.getAclChangeSetCommitTime();
//                lastACLChangeSetId = aclChangeSet.getAclChangeSetId();
//                logger.info("got last aclChangeSet from index aclCommitTime:" + aclChangeSet.getAclChangeSetCommitTime() + " aclId" + aclChangeSet.getAclChangeSetId());
//            }
//        } catch (IOException e) {
//            logger.error("problems reaching elastic search server");
//            throw e;
//        }
//    }

    public boolean track() {
        try {
            AclTx aclTx = aclStateService.getState();
            if (aclTx != null) {
                logger.info("got last aclTxn from index aclCommitTime:" + aclTx.getAclChangeSetCommitTime() + " aclId" + aclTx.getAclChangeSetId());
            }

            long lastACLChangeSetId = Optional.ofNullable(aclTx).map(AclTx::getAclChangeSetId).orElse(0L);
            long lastFromCommitTime = Optional.ofNullable(aclTx).map(AclTx::getAclChangeSetCommitTime).orElse(0L);

            logger.info("starting lastACLChangeSetId:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime + " " + new Date(lastFromCommitTime));


            AclChangeSets aclChangeSets = alfClient.getAclChangeSets(lastACLChangeSetId, lastACLChangeSetId + AclTracker.maxResults, AclTracker.maxResults);

            //step forward
            if (aclChangeSets.getMaxChangeSetId() > (lastACLChangeSetId + AclTracker.maxResults)) {
                lastACLChangeSetId += AclTracker.maxResults;
            } else {
                lastACLChangeSetId = aclChangeSets.getMaxChangeSetId();
            }


            if (aclChangeSets.getAclChangeSets().isEmpty()) {

                if (aclChangeSets.getMaxChangeSetId() <= lastACLChangeSetId) {
                    logger.info("index is up to date:" + lastACLChangeSetId + " lastFromCommitTime:" + lastFromCommitTime);
                    //+1 to prevent repeating the last transaction over and over
                    //not longer necessary when we remember last transaction id in idx
                    lastFromCommitTime = aclChangeSets.getMaxChangeSetId() + 1;
                } else {
                    logger.info("did not found new aclchangesets in last aclchangeset block from:" + (lastACLChangeSetId - AclTracker.maxResults) + " to:" + lastACLChangeSetId + " MaxChangeSetId:" + aclChangeSets.getMaxChangeSetId());
                }
                return false;
            }

            AclChangeSet last = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1);
            if (aclTx != null && aclTx.getAclChangeSetId() == aclChangeSets.getMaxChangeSetId()) {
                logger.info("nothing to do.");
                return false;
            }


            lastFromCommitTime = last.getCommitTimeMs();


            GetAclsParam param = new GetAclsParam();
            for (AclChangeSet aclChangeSet : aclChangeSets.getAclChangeSets()) {
                param.getAclChangeSetIds().add(aclChangeSet.getId());
            }


            Acls acls = alfClient.getAcls(param);

            GetPermissionsParam grp = new GetPermissionsParam();
            Map<Long, Acl> aclIdMap = acls.getAcls().stream()
                    .collect(Collectors.toMap(Acl::getId, accessControlList -> accessControlList));

            grp.setAclIds(new ArrayList<>(aclIdMap.keySet()));
            ReadersACL readers = alfClient.getReader(grp);
            Map<Long, Reader> readersMap = readers.getAclsReaders().stream()
                    .collect(Collectors.toMap(Reader::getAclId, readersList -> readersList));

            logger.debug(grp.getAclIds().toString());
            AccessControlLists accessControlLists = alfClient.getAccessControlLists(grp);
            Map<Long, AccessControlList> accessControlListMap = accessControlLists.getAccessControlLists().stream()
                    .collect(Collectors.toMap(AccessControlList::getAclId, accessControlList -> accessControlList));

            try {
                for (Acl acl : acls.getAcls()) {

                    Reader reader = readersMap.get(acl.getId());
                    if (reader.getAclId() != acl.getId()) {
                        logger.warn("reader aclid:" + reader.getAclId() + " does not match " + acl.getId());
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
                    workspaceService.updateNodesWithAcl(acl.getId(), permissionsAlf);
                }
                long lastAclChangesetid = aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1).getId();
                aclStateService.setState(new AclTx(lastAclChangesetid, lastFromCommitTime));
            } catch (IOException e) {
                logger.error("elastic search server not reachable", e);
            }

            double percentage = ((double) aclChangeSets.getAclChangeSets().get(aclChangeSets.getAclChangeSets().size() - 1).getId() - 1) / (double) aclChangeSets.getMaxChangeSetId() * 100.0d;
            DecimalFormat df = new DecimalFormat("0.00");
            logger.info("finished " + df.format(percentage) + "% lastACLChangeSetId:" + last.getId());
            return true;

        }catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }
}
