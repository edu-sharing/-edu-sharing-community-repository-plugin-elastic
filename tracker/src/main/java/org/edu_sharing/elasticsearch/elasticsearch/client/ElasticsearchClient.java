package org.edu_sharing.elasticsearch.elasticsearch.client;


import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;
import net.sourceforge.cardme.vcard.exceptions.VCardParseException;
import net.sourceforge.cardme.vcard.types.ExtendedType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.buf.StringUtils;
import org.edu_sharing.elasticsearch.alfresco.client.*;
import org.edu_sharing.elasticsearch.alfresco.client.Node;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.edu_sharing.client.NodeStatistic;
import org.edu_sharing.elasticsearch.tools.ScriptExecutor;
import org.edu_sharing.elasticsearch.tools.Tools;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.boot.json.JsonParseException;
import org.springframework.boot.json.JsonParser;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
@Component
public class ElasticsearchClient {

    public static final String CONTRIBUTOR_REGEX = "ccm:[a-zA-Z]*contributer_[a-zA-Z_-]*";
    @Value("${elastic.host}")
    String elasticHost;

    @Value("${elastic.port}")
    int elasticPort;

    @Value("${elastic.protocol}")
    String elasticProtocol;

    @Value("${elastic.socketTimeout}")
    int elasticSocketTimeout;

    @Value("${elastic.connectTimeout}")
    int elasticConnectTimeout;

    @Value("${elastic.connectionRequestTimeout}")
    int elasticConnectionRequestTimeout;

    @Value("${statistic.historyInDays}")
    int statisticHistoryInDays;

    @Value("${elastic.index.number_of_shards}")
    int indexNumberOfShards;

    @Value("${elastic.index.number_of_replicas}")
    int indexNumberOfReplicas;
    @Value("${elastic.maxCollectionChildItemsUpdateSize}")
    int maxCollectionChildItemsUpdateSize;

    Logger logger = LogManager.getLogger(ElasticsearchClient.class);

    public static String INDEX_WORKSPACE = "workspace";

    public static String INDEX_TRANSACTIONS = "transactions";

    public static String TYPE = "node";

    final static String ID_TRANSACTION = "1";

    final static String ID_ACL_CHANGESET = "2";

    final static String ID_STATISTICS_TIMESTAMP = "3";

    SimpleDateFormat statisticDateFormatter = new SimpleDateFormat("yyyy-MM-dd");


    String homeRepoId;

    @Autowired
    co.elastic.clients.elasticsearch.ElasticsearchClient client = null;

    @Autowired
    ScriptExecutor scriptExecutor;

    @Autowired
    private EduSharingClient eduSharingClient;
    private AtomicInteger nodeCounter = new AtomicInteger(0);
    private AtomicLong lastNodeCount = new AtomicLong(System.currentTimeMillis());

    @Autowired
    AlfrescoWebscriptClient alfrescoClient;

    private final SearchHitsRunner searchHitsRunner = new SearchHitsRunner(this);

    @PostConstruct
    public void init() throws IOException {
        createIndexIfNotExists(INDEX_TRANSACTIONS);
        createIndexWorkspace();
        setupElasticConfiguration();
        this.homeRepoId = eduSharingClient.getHomeRepository().getId();
    }

    private void setupElasticConfiguration() throws IOException {
        // we need to increase this value because of the large ccm/cclom model
        client.indices().putSettings(req -> req.settings(
                set -> set.index(id -> id.mapping(map -> map.totalFields(tf -> tf.limit(5000))))));
    }

    public void deleteIndex(String index) throws IOException {
        client.indices().delete(req -> req.index(index));
    }

    public void createIndexIfNotExists(String index) throws IOException {
        if (!client.indices().exists(req -> req.index(index)).value()) {
            client.indices().create(req -> req
                    .index(index)
                    .settings(set -> set
                            .index(id -> id
                                    .numberOfShards(Integer.toString(indexNumberOfShards))
                                    .numberOfReplicas(Integer.toString(indexNumberOfReplicas)))));
        }
    }

    public void updatePermissions(long dbid, Map<String, List<String>> permissions) throws IOException {
        //remove and add permissions cause its a object that would be merged
        //this.update(dbid,new Script("ctx._source.remove('permissions')"));
        this.update(dbid, Script.of(scr -> scr.inline(il -> il.source("ctx._source.permissions=null"))));
        this.update(dbid, Map.of("permissions", permissions));
    }

    public void updateNodesWithAcl(final long aclId, final Map<String, List<String>> permissions) throws IOException {
        logger.debug("starting: {} ", aclId);
        // Script script = new Script("ctx._source.permissions=null");


        UpdateByQueryResponse bulkByScrollResponse = client.updateByQuery(req -> req
                .index(INDEX_WORKSPACE)
                .query(q -> q.term(t -> t.field("aclId").value(aclId)))
                .conflicts(Conflicts.Proceed)
                .refresh(true)
                .script(scr -> scr
                        .inline(il -> il
                                .source("ctx._source.permissions=params")
                                .params(permissions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x->JsonData.of(x.getValue()))))))
        );
        logger.debug("updated: {}", bulkByScrollResponse.updated());
        List<BulkIndexByScrollFailure> bulkFailures = bulkByScrollResponse.failures();
        for (BulkIndexByScrollFailure failure : bulkFailures) {
            logger.error(failure.cause().toString(), failure.cause());
        }
    }

    public void update(long dbId, Object data) throws IOException {
        this.update(req -> req
                .index(INDEX_WORKSPACE)
                .id(Long.toString(dbId))
                .doc(data), Void.class);
    }

    public void update(long dbId, Script script) throws IOException {
        this.update(req -> req
                        .index(INDEX_WORKSPACE)
                        .id(Long.toString(dbId))
                        .script(script),
                Void.class);
    }

    private <TDocument, TPartialDocument> void update(Function<
            UpdateRequest.Builder<TDocument, TPartialDocument>,
            ObjectBuilder<UpdateRequest<TDocument, TPartialDocument>>
            > request, Class<TDocument> tDocClass) throws IOException {

        UpdateResponse<TDocument> updateResponse = client.update(request, tDocClass);
//        String index = updateResponse.index();
//        String id = updateResponse.id();
//        long version = updateResponse.version();

        if (Objects.requireNonNull(updateResponse.result()) == Result.Created) {
            logger.error("object did not exist");
        }
    }

    public void updateBulk(List<BulkOperation> updateRequests) throws IOException {
        if (updateRequests.isEmpty()) {
            return;
        }

        BulkResponse response = client.bulk(req -> req.index(INDEX_WORKSPACE).operations(updateRequests));
        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                logger.error(item.error().reason());
            }
        }
    }


    public void index(List<NodeData> nodes) throws IOException {
        logger.info("starting bulk index for {}", nodes.size());

        // TODO Missing required property 'BulkRequest.operations'

        boolean useBulkUpdate = true;

        List<BulkOperation> operations = new ArrayList<>();
        for (NodeData nodeData : nodes) {
            NodeMetadata node = nodeData.getNodeMetadata();
            Object data = get(nodeData, null).build();
            operations.add(BulkOperation.of(op -> op.index(iop -> iop
                            .index(INDEX_WORKSPACE)
                            .id(Long.toString(node.getId()))
                            .document(data))));

            if (nodeCounter.addAndGet(1) % 100 == 0) {
                logger.info("Processed " + nodeCounter.get() + " nodes (" + (System.currentTimeMillis() - lastNodeCount.get()) + "ms per last 100 nodes)");
                lastNodeCount.set(System.currentTimeMillis());
            }
        }

        if (useBulkUpdate && !operations.isEmpty()) {
            logger.info("starting bulk update:");
            BulkResponse bulkResponse = client.bulk(req->req.index(INDEX_WORKSPACE).operations(operations));
            logger.info("finished bulk update:");

            Map<Long, NodeData> collectionNodes = new HashMap<>();
            for (NodeData nodeData : nodes) {
                NodeMetadata node = nodeData.getNodeMetadata();
                if ((node.getType().equals("ccm:map") && node.getAspects().contains("ccm:collection"))
                        || (node.getType().equals("ccm:io") && !node.getAspects().contains("ccm:collection_io_reference"))) {
                    collectionNodes.put(node.getId(), nodeData);
                }
            }
            logger.info("start refresh index");
            this.refresh(INDEX_WORKSPACE);
            try {
                logger.info("start RefreshCollectionReplicas");
                for (BulkResponseItem item : bulkResponse.items()) {
                    if (item.error() != null) {
                        logger.error("Failed indexing of " + item.id());
                        logger.error("Failed indexing of " + item.error().causedBy());
                        continue;
                    }

                    Long dbId = item.id() != null ? Long.parseLong(item.id()) : null;
                    NodeData nodeData = collectionNodes.get(dbId);
                    if (nodeData != null) {
                        onUpdateRefreshUsageCollectionReplicas(nodeData.getNodeMetadata(), item.operationType() == OperationType.Update || item.operationType() == OperationType.Index);
                    }
                }
                logger.info("finished RefreshCollectionReplicas");
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                throw e;
            }

        }
        logger.info("returning");
    }

    private DataBuilder get(NodeData nodeData, DataBuilder builder) throws IOException {
        return get(nodeData, builder, null);
    }

    DataBuilder get(NodeData nodeData, DataBuilder builder, String objectName) throws IOException {

        NodeMetadata node = nodeData.getNodeMetadata();
        String storeRefProtocol = Tools.getProtocol(node.getNodeRef());
        String storeRefIdentifier = Tools.getIdentifier(node.getNodeRef());

        if (builder == null) {
            builder = new DataBuilder();
        }

        if (objectName != null) {
            builder.startObject(objectName);
        } else {
            builder.startObject();
        }


        {
            builder.field("aclId", node.getAclId());
            builder.field("txnId", node.getTxnId());
            builder.field("dbid", node.getId());

            List<String> parentUuids = Arrays.asList(node.getPaths().get(0).getApath().split("/"));
            String parentUuid = parentUuids.stream().skip(parentUuids.size() - 1).findFirst().get();
            //getAncestors() is not sorted
            String primaryParentRef = node.getAncestors().stream().filter(s -> s.contains(parentUuid)).findAny().get();
            builder.startObject("parentRef")
                    .startObject("storeRef")
                    .field("protocol", Tools.getProtocol(primaryParentRef))
                    .field("identifier", Tools.getIdentifier(primaryParentRef))
                    .endObject()
                    .field("id", Tools.getUUID(primaryParentRef))
                    .endObject();

            String id = node.getNodeRef().split("://")[1].split("/")[1];
            builder.startObject("nodeRef")
                    .startObject("storeRef")
                    .field("protocol", storeRefProtocol)
                    .field("identifier", storeRefIdentifier)
                    .endObject()
                    .field("id", id)
                    .endObject();

            builder.field("owner", node.getOwner());
            builder.field("type", node.getType());

            scriptExecutor.addCustomPropertiesByScript(builder, nodeData);

            //valuespaces
            if (!nodeData.getValueSpaces().isEmpty()) {
                builder.startObject("i18n");
                for (Map.Entry<String, Map<String, List<String>>> entry : nodeData.getValueSpaces().entrySet()) {
                    String language = entry.getKey().split("-")[0];
                    builder.startObject(language);
                    for (Map.Entry<String, List<String>> valuespace : entry.getValue().entrySet()) {

                        String key = CCConstants.getValidLocalName(valuespace.getKey());
                        if (key != null) {
                            builder.field(key, valuespace.getValue());
                        } else {
                            builder.field(valuespace.getKey(), valuespace.getValue());
                            // logger.error("unknown valuespace property: " + valuespace.getKey());
                        }
                    }
                    builder.endObject();
                }
                builder.endObject();
            }

            if (node.getPaths() != null && !node.getPaths().isEmpty()) {
                addNodePath(builder, node);
            }

            builder.startObject("permissions");
            builder.field("read", nodeData.getReader().getReaders());
            for (Map.Entry<String, List<String>> entry : nodeData.getPermissions().entrySet()) {
                builder.field(entry.getKey(), entry.getValue());
            }
            builder.endObject();

            //content
            /**
             *     "{http://www.alfresco.org/model/content/1.0}content": {
             *    "contentId": "279",
             *    "encoding": "UTF-8",
             *    "locale": "de_DE_",
             *    "mimetype": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
             *    "size": "8385"
             * },
             */
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> content = (LinkedHashMap<String, Object>) node.getProperties().get("{http://www.alfresco.org/model/content/1.0}content");
            if (content != null) {

                builder.startObject("content");
                builder.field("contentId", content.get("contentId"));
                builder.field("encoding", content.get("encoding"));
                builder.field("locale", content.get("locale"));
                builder.field("mimetype", content.get("mimetype"));
                builder.field("size", content.get("size"));
                if (nodeData.getFullText() != null) {
                    builder.field("fulltext", nodeData.getFullText());
                }
                builder.endObject();
            }


            HashMap<String, Serializable> contributorProperties = new HashMap<>();
            builder.startObject("properties");
            for (Map.Entry<String, Serializable> prop : node.getProperties().entrySet()) {

                String key = CCConstants.getValidLocalName(prop.getKey());
                if (key == null) {
                    logger.error("unknown namespace: " + prop.getKey());
                    continue;
                }

                Serializable value = prop.getValue();
                if (key.matches(CONTRIBUTOR_REGEX)) {
                    if (value != null) {
                        contributorProperties.put(key, value);
                    }
                }

                if (prop.getValue() instanceof List) {
                    List listvalue = (List) prop.getValue();

                    //i.e. cm:title
                    if (!listvalue.isEmpty() && listvalue.get(0) instanceof Map) {
                        value = getMultilangValue(listvalue);
                    }

                    //i.e. cclom:general_keyword
                    if (!listvalue.isEmpty() && listvalue.get(0) instanceof List) {
                        List<String> mvValue = new ArrayList<String>();
                        for (Object l : listvalue) {
                            String mlv = getMultilangValue((List) l);
                            if (mlv != null) {
                                mvValue.add(mlv);
                            }
                        }
                        if (!mvValue.isEmpty()) {
                            value = (Serializable) mvValue;
                        }//fix: mapper_parsing_exception Preview of field's value: '{locale=de_}']] (empty keyword)
                        else {
                            logger.info("fallback to \\”\\” for prop " + key + " v:" + value);
                            value = "";
                        }
                    }
                }
                if ("cm:modified".equals(key) || "cm:created".equals(key)) {

                    if (prop.getValue() != null) {
                        value = Date.from(Instant.parse((String) prop.getValue())).getTime();
                    }
                }

                //prevent Elasticsearch exception failed to parse field's value: '1-01-07T01:00:00.000+01:00'
                if ("ccm:replicationmodified".equals(key)) {
                    if (prop.getValue() != null) {
                        value = prop.getValue().toString();
                    }
                }

                //elastic maps this on date field, it gets a  failed to parse field exception when it's empty
                if ("ccm:replicationsourcetimestamp".equals(key)) {
                    if (value != null && value.toString().trim().isEmpty()) {
                        value = null;
                    }
                }

                if ("ccm:mediacenter".equals(key)) {
                    ArrayList<Map<String, Object>> mediacenters = null;
                    if (value != null && !value.toString().trim().isEmpty()) {
                        JsonParser jp = new BasicJsonParser();
                        @SuppressWarnings("unchecked")
                        List<String> mzStatusList = (List<String>) value;
                        ArrayList<Map<String, Object>> result = new ArrayList<>();
                        for (String mzStatus : mzStatusList) {
                            try {
                                result.add(jp.parseMap(mzStatus));
                            } catch (JsonParseException e) {
                                logger.error(e.getMessage());
                            }
                        }
                        if (!result.isEmpty()) {
                            value = result;
                            mediacenters = result;
                        }
                    }

                    if (mediacenters != null) {
                        builder.startObject("ccm:mediacenter_sort");
                        for (Map<String, Object> mediacenter : mediacenters) {
                            builder.startObject((String) mediacenter.get("name"));
                            builder.field("activated", mediacenter.get("activated"));
                            builder.endObject();
                        }
                        builder.endObject();
                    }

                }

                if (value != null) {

                    try {
                        builder.field(key, value);
                    } catch (Exception e) {
                        logger.error("error parsing value field:" + key + "v" + value, e);
                    }
                }
            }
            builder.endObject();

            builder.field("aspects", node.getAspects());

            if (!contributorProperties.isEmpty()) {
                VCardEngine vcardEngine = new VCardEngine();
                builder.startArray("contributor");
                for (Map.Entry<String, Serializable> entry : contributorProperties.entrySet()) {
                    if (entry.getValue() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> val = (List<String>) entry.getValue();
                        for (String v : val) {
                            try {
                                if (v == null) continue;
                                VCard vcard = vcardEngine.parse(v);
                                if (vcard != null) {

                                    builder.startObject();
                                    builder.field("property", entry.getKey());
                                    if (vcard.getN() != null) {
                                        builder.field("firstname", vcard.getN().getGivenName());
                                        builder.field("lastname", vcard.getN().getFamilyName());
                                    }
                                    if (vcard.getEmails() != null && !vcard.getEmails().isEmpty()) {
                                        builder.field("email", vcard.getEmails().get(0).getEmail());
                                    }
                                    if (vcard.getUid() != null) {
                                        builder.field("uuid", vcard.getUid().getUid());
                                    }
                                    if (vcard.getUrls() != null && !vcard.getUrls().isEmpty()) {
                                        builder.field("url", vcard.getUrls().get(0).getRawUrl());
                                    }
                                    if (vcard.getOrg() != null) {
                                        builder.field("org", vcard.getOrg().getOrgName());
                                    }

                                    List<ExtendedType> extendedTypes = vcard.getExtendedTypes();
                                    if (extendedTypes != null) {
                                        for (ExtendedType et : extendedTypes) {
                                            if (et.getExtendedValue() != null && !et.getExtendedValue().trim().isEmpty()) {
                                                builder.field(et.getExtendedName(), et.getExtendedValue());
                                            }
                                        }
                                    }


                                    builder.field("vcard", v);
                                    builder.endObject();
                                }
                            } catch (VCardParseException e) {
                                logger.error(e.getMessage(), e);
                            } catch (NullPointerException e) {
                                logger.error("node: " + id + " " + e.getMessage(), e);
                            }
                        }

                    }
                }
                builder.endArray();
            }
            if (nodeData.getNodePreview() != null) {
                builder.startObject("preview").
                        field("mimetype", nodeData.getNodePreview().getMimetype()).
                        field("type", nodeData.getNodePreview().getType()).
                        field("icon", nodeData.getNodePreview().isIcon()).
                        field("small", nodeData.getNodePreview().getSmall()).
                        //field("large", nodeData.getNodePreview().getLarge()).
                                endObject();
            }

            if (!nodeData.getChildren().isEmpty()) {
                builder.startArray("children");
                for (NodeData child : nodeData.getChildren()) {
                    get(child, builder);
                }
                builder.endArray();

                Map<Date, List<Double>> ratingsAtDay = new HashMap<>();
                Map<Date, Double> ratingsAtDayAverage = new HashMap<>();
                double ratingAll = 0;
                for (NodeData child : nodeData.getChildren()) {
                    if ("ccm:rating".equals(child.getNodeMetadata().getType())) {
                        Date modified = Date.from(Instant.parse((String) child.getNodeMetadata().getProperties().get(CCConstants.getValidGlobalName("cm:modified"))));
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(modified);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.clear(Calendar.MINUTE);
                        cal.clear(Calendar.SECOND);
                        cal.clear(Calendar.MILLISECOND);
                        Date date = cal.getTime();
                        Double rating = Double.parseDouble((String) child.getNodeMetadata().getProperties().get(CCConstants.getValidGlobalName("ccm:rating_value")));
                        List<Double> dayRatings = ratingsAtDay.computeIfAbsent(date, k -> new ArrayList<>());
                        dayRatings.add(rating);
                    }
                }
                //average at day
                for (Map.Entry<Date, List<Double>> entry : ratingsAtDay.entrySet()) {
                    ratingsAtDayAverage.put(entry.getKey(), entry.getValue().stream().mapToDouble(x -> x).summaryStatistics().getAverage());
                }
                //average all
                ratingAll = ratingsAtDayAverage.values().stream().mapToDouble(x -> x).summaryStatistics().getAverage();

                for (Map.Entry<Date, Double> rating : ratingsAtDayAverage.entrySet()) {
                    builder.field("statistic_RATING_" + statisticDateFormatter.format(rating.getKey()), rating.getValue());
                }
                if ("ccm:io".equals(nodeData.getNodeMetadata().getType())) {
                    builder.field("statistic_RATING_null", ratingAll);
                }
            }
        }

        if (nodeData instanceof NodeDataProposal) {
            builder = getProposalData((NodeDataProposal) nodeData, builder);
        }

        builder.endObject();

        return builder;
    }

    private DataBuilder getProposalData(NodeDataProposal nodeData, DataBuilder builder) throws IOException {
        if (nodeData.getCollection() != null) {
            builder.startArray("collections");
            builder = get(nodeData.getCollection(), builder);
            builder.endArray();
        }
        if (nodeData.getOriginal() != null) {
            builder = get(nodeData.getOriginal(), builder, "original");
        }
        return builder;
    }

    private void addNodePath(DataBuilder builder, NodeMetadata node) throws IOException {
        String[] pathEle = node.getPaths().get(0).getApath().split("/");
        builder.field("path", Arrays.copyOfRange(pathEle, 1, pathEle.length));
        builder.field("fullpath", StringUtils.join(Arrays.asList(Arrays.copyOfRange(pathEle, 1, pathEle.length)), '/'));
    }

    public void refresh(String index) throws IOException {
        logger.debug("starting");
        client.indices().refresh(req -> req.index(index));
        logger.debug("returning");
    }

    public DataBuilder indexCollections(NodeMetadata usageOrProposal) throws IOException {

        String nodeIdCollection = null;
        String nodeIdIO = null;

        if (!(usageOrProposal.getType().equals("ccm:usage") || usageOrProposal.getType().equals("ccm:collection_proposal"))) {
            throw new IOException("wrong type:" + usageOrProposal.getType());
        }

        if (usageOrProposal.getType().equals("ccm:usage")) {
            String propertyUsageAppId = "{http://www.campuscontent.de/model/1.0}usageappid";
            String propertyUsageCourseId = "{http://www.campuscontent.de/model/1.0}usagecourseid";
            String propertyUsageParentNodeId = "{http://www.campuscontent.de/model/1.0}usageparentnodeid";

            nodeIdCollection = (String) usageOrProposal.getProperties().get(propertyUsageCourseId);
            nodeIdIO = (String) usageOrProposal.getProperties().get(propertyUsageParentNodeId);
            String usageAppId = (String) usageOrProposal.getProperties().get(propertyUsageAppId);

            //check if it is an collection usage
            if (!homeRepoId.equals(usageAppId)) {
                return null;
            }
        }
        if (usageOrProposal.getType().equals("ccm:collection_proposal")) {
            List<String> parentUuids = Arrays.asList(usageOrProposal.getPaths().get(0).getApath().split("/"));
            nodeIdCollection = parentUuids.stream().skip(parentUuids.size() - 1).findFirst().get();
            Serializable ioNodeRef = usageOrProposal.getProperties().get("{http://www.campuscontent.de/model/1.0}collection_proposal_target");
            if (ioNodeRef == null) {
                logger.warn("no proposal target found for: " + usageOrProposal.getNodeRef());
                return null;
            }
            nodeIdIO = Tools.getUUID(ioNodeRef.toString());
        }

        final String finalNodeIdCollection = nodeIdCollection;
        final String finalnodeIdIO = nodeIdIO;
        Query collectionQuery = Query.of(q -> q.term(t -> t.field("properties.sys:node-uuid").value(finalNodeIdCollection)));
        Query ioQuery = Query.of(q -> q.term(t -> t.field("properties.sys:node-uuid").value(finalnodeIdIO)));

        HitsMetadata<Map> searchHitsCollection = this.search(INDEX_WORKSPACE, collectionQuery, 0, 1);
        if (searchHitsCollection == null || searchHitsCollection.total().value() == 0) {
            logger.warn("no collection found for: " + nodeIdCollection);
            return null;
        }
        Hit<Map> searchHitCollection = searchHitsCollection.hits().get(0);

        HitsMetadata<Map> ioSearchHits = this.search(INDEX_WORKSPACE, ioQuery, 0, 1);
        if (ioSearchHits == null || ioSearchHits.total().value() == 0) {
            logger.warn("no io found for: " + nodeIdIO);
            return null;
        }

        Hit<Map> hitIO = ioSearchHits.hits().get(0);

        Map propsIo = (Map) hitIO.source().get("properties");
        Map propsCollection = (Map) searchHitCollection.source().get("properties");


        logger.info("adding collection data: " + propsCollection.get("cm:name") + " " + propsCollection.get("sys:node-dbid") + " IO: " + propsIo.get("cm:name") + " " + propsIo.get("sys:node-dbid"));

        List<Map<String, Object>> collections = (List<Map<String, Object>>) hitIO.source().get("collections");
        DataBuilder builder = new DataBuilder();
        builder.startObject();
        {
            builder.startArray("collections");
            if (collections != null && collections.size() > 0) {
                for (Map<String, Object> collection : collections) {
                    boolean colIsTheSame = searchHitCollection.source().get("dbid").equals(collection.get("dbid"));

                    Map<String, Object> relation = (Map<String, Object>) collection.get("relation");
                    if (relation != null) {
                        long dbidRelation = ((Number) (relation.get("dbid"))).longValue();
                        if (!colIsTheSame || (colIsTheSame && dbidRelation != usageOrProposal.getId())) {
                            builder.startObject();
                            for (Map.Entry<String, Object> entry : collection.entrySet()) {
                                if (entry.getKey().equals("children")) continue;
                                builder.field(entry.getKey(), entry.getValue());
                            }
                            builder.endObject();
                        }
                    }
                }
            }

            builder.startObject();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) searchHitCollection.source()).entrySet()) {
                if (entry.getKey().equals("children")) continue;
                builder.field(entry.getKey(), entry.getValue());
            }

            /**
             * check performance, if this call is to slow we could build the metadata structure by hand (instead using get)
             * for usages: we could resolve the collection_ref object to have alternate metadata and store it in relation field
             * but it would be another call and could make the indexing process slower.
             * for the future: maybe it's better to react on collection_ref object index actions than usage index actions
             */
            get(alfrescoClient.getNodeData(List.of(usageOrProposal)).get(0), builder, "relation");

            builder.endObject();
            builder.endArray();
        }
        builder.endObject();
        int dbid = Integer.parseInt(hitIO.id());
        this.update(dbid, builder.build());
        this.refresh(INDEX_WORKSPACE);
        return builder;
    }

    /**
     * checks if its a collection usage by searching for collections.usagedbid, and removes replicated collection object
     *
     * @param nodes
     * @throws IOException
     */
    public void beforeDeleteCleanupCollectionReplicas(List<Node> nodes) throws IOException {
        logger.info("starting: " + nodes.size());

        if (nodes.isEmpty()) {
            logger.info("returning 0");
            return;
        }

        List<BulkOperation> updateRequests = new ArrayList<>();
        for (Node node : nodes) {

            Query collectionCheckQuery = null;
            /**
             * try it is a usage or proposal
             */
            Query queryUsage = Query.of(q -> q.term(t -> t.field("collections.relation.dbid").value(node.getId())));
            HitsMetadata<Map> searchHitsIO = this.search(INDEX_WORKSPACE, queryUsage, 0, 1);
            if (searchHitsIO.total().value() > 0) {
                collectionCheckQuery = queryUsage;
            }

            /**
             * try it is an collection
             */
            Query queryCollection = Query.of(q -> q.term(t -> t.field("collections.dbid").value(node.getId())));
            if (collectionCheckQuery == null) {
                searchHitsIO = this.search(INDEX_WORKSPACE, queryCollection, 0, 1);
                if (!searchHitsIO.hits().isEmpty()) {
                    collectionCheckQuery = queryCollection;
                }
            }


            //nothing to cleanup
            if (collectionCheckQuery == null) {
                continue;
            }
            boolean collectionDeleted = collectionCheckQuery.equals(queryCollection);
            logger.info("cleanup collection cause " + (collectionDeleted ? "collection deleted" : "usage/proposal deleted"));

            searchHitsRunner.run(collectionCheckQuery, hitIO -> {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> collections = (List<Map<String, Object>>) hitIO.source().get("collections");
                DataBuilder builder = new DataBuilder();
                builder.startObject();
                {
                    builder.startArray("collections");
                    if (collections != null && collections.size() > 0) {
                        for (Map<String, Object> collection : collections) {
                            long nodeDbId = node.getId();

                            Object collCeckAttValue = null;
                            if (collectionDeleted) {
                                collCeckAttValue = collection.get("dbid");
                            } else {
                                collCeckAttValue = ((HashMap) collection.get("relation")).get("dbid");
                            }

                            if (collCeckAttValue == null) {
                                logger.error("replicated collection " + collection.get("dbid") + " does not have a property to check will leave it out");
                                continue;
                            }
                            long collectionAttValue = Long.parseLong(collCeckAttValue.toString());
                            if (nodeDbId != collectionAttValue) {
                                builder.startObject();
                                for (Map.Entry<String, Object> entry : collection.entrySet()) {
                                    builder.field(entry.getKey(), entry.getValue());
                                }
                                builder.endObject();
                            }
                        }
                    }
                    builder.endArray();
                }
                builder.endObject();
                int dbid = Integer.parseInt(hitIO.id());

                updateRequests.add(BulkOperation.of(op -> op
                        .update(up -> up.index(INDEX_WORKSPACE)
                                .id(Long.toString(dbid))
                                .action(a -> a.doc(builder.build())))));
            });
        }
        this.updateBulk(updateRequests);
        logger.info("returning");
    }

    /**
     * update ios collection metdata, that are containted inside a collection
     *
     * @param nodeCollection
     * @throws IOException
     * @deprecated
     */
    private void _onUpdateRefreshCollectionReplicas(Node nodeCollection) throws IOException {
        List<BulkOperation> updateRequests = new ArrayList<>();
        Query queryCollection = Query.of(q -> q.term(t -> t.field("collections.dbid").value(nodeCollection.getId())));

        searchHitsRunner.run(queryCollection, hit -> {
            logger.info("updating collection data for:" + hit.source().get("dbid"));
            Query collectionQuery = Query.of(q -> q.term(t -> t.field("properties.sys:node-uuid").value(Tools.getUUID(nodeCollection.getNodeRef()))));
            HitsMetadata<Map> searchHitsCollection = null;
            try {
                searchHitsCollection = this.search(INDEX_WORKSPACE, collectionQuery, 0, 1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (searchHitsCollection == null || searchHitsCollection.total().value() == 0) {
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> collectionReplicas = (List<Map<String, Object>>) hit.source().get("collections");
            DataBuilder builder = new DataBuilder();
            builder.startObject();
            {
                builder.startArray("collections");

                for (Map<String, Object> collectionReplica : collectionReplicas) {
                    long collReplDbid = ((Number) collectionReplica.get("dbid")).longValue();
                    if (collReplDbid == nodeCollection.getId()) {
                        builder.startObject();
                        //noinspection unchecked
                        for (Map.Entry<String, Object> entry : ((Map<String, Object>) searchHitsCollection.hits().get(0).source()).entrySet()) {
                            builder.field(entry.getKey(), entry.getValue());
                        }
                        builder.endObject();
                    } else {
                        builder.startObject();
                        for (Map.Entry<String, Object> entry : collectionReplica.entrySet()) {
                            builder.field(entry.getKey(), entry.getValue());
                        }
                        builder.endObject();
                    }
                }

                builder.endArray();
            }
            builder.endObject();
            int dbid = Integer.parseInt(hit.id());
            updateRequests.add(BulkOperation.of(op -> op.update(u -> u.index(INDEX_WORKSPACE).id(Long.toString(dbid)).action(a -> a.doc(builder.build())))));
        });
        this.updateBulk(updateRequests);
    }

    private void onUpdateRefreshUsageCollectionReplicas(NodeMetadata node, boolean update) throws IOException {

        final String query;
        final String queryProposal;
        if ("ccm:map".equals(node.getType())) {
            query = "properties.ccm:usagecourseid.keyword";
            queryProposal = "parentRef.id";
        } else if ("ccm:io".equals(node.getType())) {
            query = "properties.ccm:usageparentnodeid.keyword";
            queryProposal = "properties.ccm:collection_proposal_target.keyword";
        } else {
            logger.info("can not handle collections for type:" + node.getType());
            return;
        }

        logger.info("updateing collections for " + node.getType() + " " + node.getId());

        //find usages for collection
        Query queryUsages = Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t.field(query).value(Tools.getUUID(node.getNodeRef()))))
                .must(m -> m.term(t -> t.field("type").value("ccm:usage")))));

        final Query queryProposalBase = ("ccm:io".equals(node.getType()))
                ? Query.of(q -> q.term(t -> t.field(queryProposal).value(node.getNodeRef())))
                : Query.of(q -> q.term(t -> t.field(queryProposal).value(Tools.getUUID(node.getNodeRef()))));

        Query queryProposals = Query.of(q -> q.bool(b -> b.must(queryProposalBase).must(m -> m.term(t -> t.field("type").value("ccm:collection_proposal")))));

        Consumer<Hit<Map>> action = hit -> {
            long dbId = ((Number) hit.source().get("dbid")).longValue();
            GetNodeMetadataParam param = new GetNodeMetadataParam();
            param.setNodeIds(Arrays.asList(new Long[]{dbId}));
            List<NodeMetadata> nodeMetadataByIds = alfrescoClient.getNodeMetadataByIds(List.of(dbId));
            if (nodeMetadataByIds == null || nodeMetadataByIds.isEmpty()) {
                logger.error("could not find usage/proposal object in alfresco with dbid:" + dbId);
                return;
            }

            NodeMetadata usage = nodeMetadataByIds.get(0);
            logger.info("Is update: {}", update);

            logger.info("running indexCollections for usage: " + dbId);
            try {
                indexCollections(usage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        searchHitsRunner.run(queryUsages, 5, update ? maxCollectionChildItemsUpdateSize : null, action);
        searchHitsRunner.run(queryProposals, 5, update ? maxCollectionChildItemsUpdateSize : null, action);
    }

    private String getMultilangValue(List listvalue) {
        if (listvalue.size() > 1) {
            // find german value i.e for Documents/Images edu folder
            String value = null;
            String defaultValue = null;

            for (Object o : listvalue) {
                Map m = (Map) o;
                //default is {key="locale",value="de"},{key="value",value="Deutsch"}
                if (m.size() > 2) {
                    throw new RuntimeException("language map has only one value");
                }
                defaultValue = (String) m.get("value");
                if ("de".equals(m.get("locale")) || "de_".equals(m.get("locale"))) {
                    value = (String) m.get("value");
                }
            }
            if (value == null) value = defaultValue;
            return value;
        } else if (listvalue.size() == 1) {
            Map multilangValue = (Map) listvalue.get(0);
            return (String) multilangValue.get("value");
        } else {
            return null;
        }
    }

    public void setTransaction(String index, long txnCommitTime, long transactionId) throws IOException {

        DataBuilder builder = new DataBuilder();
        builder.startObject();
        {
            builder.field("txnId", transactionId);
            builder.field("txnCommitTime", txnCommitTime);
        }
        builder.endObject();

        setNode(index, ID_TRANSACTION, builder);
    }

    public void setTransaction(long txnCommitTime, long transactionId) throws IOException {
        setTransaction(INDEX_TRANSACTIONS, txnCommitTime, transactionId);
    }

    private void setNode(String index, String id, DataBuilder builder) throws IOException {

        IndexResponse indexResponse = client.index(IndexRequest.of(req -> req.index(index).id(id).document(builder.build())));

        if (indexResponse.result() == Result.Created) {
            logger.debug("created node in elastic:" + builder);
        } else if (indexResponse.result() == Result.Updated) {
            logger.debug("updated node in elastic:" + builder);
        }

        ShardStatistics shardInfo = indexResponse.shards();
        if (shardInfo.total().longValue() != shardInfo.successful().longValue()) {
            logger.debug("shardInfo.total().longValue() " + shardInfo.total().longValue() + "!=" + "shardInfo.successful().longValue():" + shardInfo.successful().longValue());
        }

        if (shardInfo.failed().longValue() > 0) {
            for (ShardFailure failure : shardInfo.failures()) {
                String reason = failure.reason().reason();
                logger.error(failure.node() + " reason:" + reason);
            }
        }
    }

    public GetResponse<Map> get(String index, String id) throws IOException {
        return client.get(GetRequest.of(req -> req.index(index).id(id)), Map.class);
    }

    public boolean exists(String index, String id) throws IOException {
        return client.exists(ExistsRequest.of(req -> req.index(index).id(id))).value();
    }

    public Tx getTransaction(String index) throws IOException {

        GetResponse<Map> resp = this.get(index, ID_TRANSACTION);
        Tx transaction = null;
        if (resp.found()) {
            transaction = new Tx();
            transaction.setTxnCommitTime(((Number) resp.source().get("txnCommitTime")).longValue());
            transaction.setTxnId(((Number) resp.source().get("txnId")).longValue());
        }

        return transaction;
    }

    public Tx getTransaction() throws IOException {
        return getTransaction(INDEX_TRANSACTIONS);
    }


    public void setACLChangeSet(final long aclChangeSetTime, final long aclChangeSetId) throws IOException {
        DataBuilder builder = new DataBuilder();
        builder.startObject();
        {
            builder.field("aclChangeSetId", aclChangeSetId);
            builder.field("aclChangeSetCommitTime", aclChangeSetTime);
        }
        builder.endObject();

        setNode(INDEX_TRANSACTIONS, ID_ACL_CHANGESET, builder);
    }

    public ACLChangeSet getACLChangeSet() throws IOException {
        GetResponse<Map> resp = this.get(INDEX_TRANSACTIONS, ID_ACL_CHANGESET);

        ACLChangeSet aclChangeSet = null;
        if (resp.found()) {
            aclChangeSet = new ACLChangeSet();
            aclChangeSet.setAclChangeSetCommitTime(Long.parseLong(resp.source().get("aclChangeSetCommitTime").toString()));
            aclChangeSet.setAclChangeSetId((int) resp.source().get("aclChangeSetId"));
        }

        return aclChangeSet;
    }

    public void setStatisticTimestamp(long statisticTimestamp, boolean allInIndex) throws IOException {
        DataBuilder builder = new DataBuilder();
        builder.startObject();
        {
            builder.field("statisticTimestamp", statisticTimestamp);
            builder.field("allInIndex", allInIndex);
        }
        builder.endObject();
        setNode(INDEX_TRANSACTIONS, ID_STATISTICS_TIMESTAMP, builder);
    }

    public StatisticTimestamp getStatisticTimestamp() throws IOException {
        GetResponse<Map> resp = this.get(INDEX_TRANSACTIONS, ID_STATISTICS_TIMESTAMP);
        StatisticTimestamp sTs = null;
        if (resp.found()) {
            sTs = new StatisticTimestamp();
            sTs.setStatisticTimestamp(Long.parseLong(resp.source().get("statisticTimestamp").toString()));
            Boolean allInIndex = (Boolean) resp.source().get("allInIndex");
            sTs.setAllInIndex((allInIndex == null) ? false : allInIndex);
        }
        return sTs;
    }

    public void delete(List<Node> nodes) throws IOException {
        logger.info("starting size:" + nodes.size());
        if (!nodes.isEmpty()) {
            BulkResponse response = client.bulk(BulkRequest.of(req -> req
                    .index(INDEX_WORKSPACE)
                    .operations(op -> {
                        nodes.forEach(n -> op.delete(d -> d.index(INDEX_WORKSPACE).id(Long.toString(n.getId()))));
                        return op;
                    })));

            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    logger.error(item.error().causedBy());
                }
            }
        }
        logger.info("returning");
    }

    /**
     * @throws IOException
     * @TODO
     */
    public void createIndexWorkspace() throws IOException {
        try {

            if (client.indices().exists(co.elastic.clients.elasticsearch.indices.ExistsRequest.of(req -> req.index(INDEX_WORKSPACE))).value()) {
                return;
            }

            CreateIndexRequest indexRequest = CreateIndexRequest.of(req -> req
                    .index(INDEX_WORKSPACE)
                    .settings(this::getIndexSettings)
                    .mappings(this::getMappings));
            client.indices().create(indexRequest);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    IndexSettings.Builder getIndexSettings(IndexSettings.Builder s) {
        return s.index(id -> id
                .numberOfShards(Integer.toString(indexNumberOfShards))
                .numberOfReplicas(Integer.toString(indexNumberOfReplicas))
        ).analysis(analysis -> analysis
                .analyzer("trigram", a -> a
                        .custom(c -> c
                                .tokenizer("standard")
                                .filter("lowercase", "shingle")))
                .analyzer("reverse", a -> a
                        .custom(c -> c
                                .tokenizer("standard")
                                .filter("lowercase", "reverse")))
                .filter("shingle", f -> f
                        .definition(def -> def
                                .shingle(shingle -> shingle
                                        .minShingleSize("2")
                                        .maxShingleSize("3")))));
    }

    ObjectBuilder<TypeMapping> getMappings(TypeMapping.Builder mapping) {
        //noinspection unchecked
        return mapping.dynamic(DynamicMapping.True)
                .numericDetection(true)
                .dynamicTemplates(Map.of("aggregated_type", DynamicTemplate.of(dt -> dt
                                .matchMappingType("string")
                                .pathMatch("properties_aggregated.*")
                                .mapping(mp -> mp.keyword(kw -> kw.ignoreAbove(256).store(true))))),

                        Map.of("convert_date", DynamicTemplate.of(dt -> dt
                                .matchMappingType("date")
                                .pathMatch("*properties.*")
                                .mapping(mp -> mp.text(t -> t.store(true)
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256)))
                                        .fields("date", f -> f.date(v -> v.ignoreMalformed(true))))))),

                        Map.of("convert_numeric_long", DynamicTemplate.of(dt -> dt
                                .matchMappingType("long")
                                .pathMatch("*properties.*")
                                .mapping(mp -> mp.text(t -> t.store(true)
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256)))
                                        .fields("number", f -> f.long_(v -> v.ignoreMalformed(true))))))),

                        Map.of("convert_numeric_double", DynamicTemplate.of(dt -> dt
                                .matchMappingType("double")
                                .pathMatch("*properties.*")
                                .mapping(mp -> mp.text(t -> t.store(true)
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256)))
                                        .fields("number", f -> f.float_(v -> v.ignoreMalformed(true))))))),

                        Map.of("convert_date_aggregated", DynamicTemplate.of(dt -> dt
                                .matchMappingType("date")
                                .pathMatch("*properties_aggregated.*")
                                .mapping(mp -> mp.text(t -> t.store(true)
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256)))
                                        .fields("date", f -> f.date(v -> v.ignoreMalformed(true))))))),

                        Map.of("convert_numeric_long_aggregated", DynamicTemplate.of(dt -> dt
                                .matchMappingType("long")
                                .pathMatch("*properties_aggregated.*")
                                .mapping(mp -> mp.text(t -> t.store(true)
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256)))
                                        .fields("number", f -> f.long_(v -> v.ignoreMalformed(true))))))),

                        Map.of("convert_numeric_double_aggregated", DynamicTemplate.of(dt -> dt
                                .matchMappingType("double")
                                .pathMatch("*properties_aggregated.*")
                                .mapping(mp -> mp.text(t -> t.store(true)
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256)))
                                        .fields("number", f -> f.float_(v -> v.ignoreMalformed(true))))))),

                        Map.of("copy_facettes", DynamicTemplate.of(dt -> dt
                                .matchMappingType("string")
                                .pathMatch("*properties.*")
                                .mapping(mp -> mp.text(t -> t.copyTo("properties_aggregated.{name}")
                                        .fields("keyword", f -> f.keyword(kw -> kw.ignoreAbove(256))))))),

                        Map.of("statistics_rating", DynamicTemplate.of(dt -> dt
                                .pathMatch("statistic_RATING_*")
                                .mapping(mp -> mp.float_(f -> f)))),

                        Map.of("statistics_generic", DynamicTemplate.of(dt -> dt
                                .pathMatch("statistic_*")
                                .mapping(mp -> mp.long_(l -> l))))
                )
                .properties("aclId", prop -> prop.long_(v -> v))
                .properties("txnId", prop -> prop.long_(v -> v))
                .properties("dbid", prop -> prop.long_(v -> v))
                .properties("parentRef", parentRefProp -> parentRefProp
                        .object(parentRefObj -> parentRefObj
                                .properties("id", storeRefProp -> storeRefProp.keyword(v -> v))
                                .properties("storeRef", storeRefProp -> storeRefProp
                                        .object(storeRefObj -> storeRefObj
                                                .properties("protocol", protProp -> protProp.keyword(v -> v))
                                                .properties("identifier", idProp -> idProp.keyword(v -> v))))))
                .properties("nodeRef", parentRefProp -> parentRefProp
                        .object(parentRefObj -> parentRefObj
                                .properties("id", storeRefProp -> storeRefProp.keyword(v -> v))
                                .properties("storeRef", storeRefProp -> storeRefProp
                                        .object(storeRefObj -> storeRefObj
                                                .properties("protocol", protProp -> protProp.keyword(v -> v))
                                                .properties("identifier", idProp -> idProp.keyword(v -> v))))))

                .properties("owner", prop -> prop.keyword(v -> v))
                .properties("type", prop -> prop.keyword(v -> v))
                .properties("path", prop -> prop.keyword(v -> v))
                .properties("fullpath", prop -> prop.keyword(v -> v))
                .properties("permissions", prop -> prop
                        .object(permObj -> permObj
                                .properties("read", readProp -> readProp.keyword(v -> v))))
                .properties(addContentDefinition())
                .properties("properties", propProp -> propProp
                        .object(propObj -> propObj
                                .properties("ccm:original", prop -> prop.keyword(v -> v))
                                .properties("cclom:location", prop -> prop.keyword(v -> v))
                                .properties("sys:node-uuid", prop -> prop.keyword(v -> v))
                                .properties("cclom:format", prop -> prop.keyword(v -> v))
                                .properties("cm:versionLabel", prop -> prop.keyword(v -> v))
                                .properties("cclom:title", titleProp -> titleProp
                                        .text(t -> t.copyTo("properties_aggregated.cclom:title")
                                                .fields("keyword", prop -> prop.keyword(v -> v.ignoreAbove(256)))
                                                .fields("trigram", prop -> prop.text(v -> v.analyzer("trigram")))
                                                .fields("reverse", prop -> prop.text(v -> v.analyzer("reverse")))))))
                .properties("children", childProp -> childProp
                        .object(childObj -> childObj
                                .properties("type", prop -> prop.keyword(v -> v))
                                .properties("aspects", prop -> prop.keyword(v -> v))
                                .properties(addContentDefinition())
                        ))
                .properties("aspects", prop -> prop.keyword(v -> v))
                .properties("collections", colProp -> colProp
                        .object(colObj -> colObj
                                .properties("dbid", prop -> prop.long_(v -> v))
                                .properties("aclId", prop -> prop.long_(v -> v))
                        ))
                .properties("preview", previewProp -> previewProp
                        .object(previewObj -> previewObj
                                .properties("mimetype", prop -> prop.keyword(v -> v))
                                .properties("type", prop -> prop.keyword(v -> v))
                                .properties("icon", prop -> prop.boolean_(v -> v))
                                .properties("small", prop -> prop.binary(v -> v))));
    }

    private Map<String, Property> addContentDefinition() {
        return Map.of("content", Property.of(contentProps -> contentProps
                .object(obj -> obj
                        .properties("fulltext", prop -> prop.text(v -> v))
                        .properties("contentId", prop -> prop.long_(v -> v))
                        .properties("size", prop -> prop.long_(v -> v))
                        .properties("encoding", prop -> prop.keyword(v -> v))
                        .properties("locale", prop -> prop.keyword(v -> v))
                        .properties("mimetype", prop -> prop.keyword(v -> v)))));
    }

    public HitsMetadata<Map> search(String index, Query queryBuilder, int from, int size) throws IOException {
        return this.search(index, queryBuilder, from, size, null);
    }

    public HitsMetadata<Map> search(String index, Query query, int from, int size, List<String> excludes) throws IOException {
        SearchResponse<Map> searchResponse = client.search(req -> {
                    req.index(index)
                            .query(query)
                            .from(from)
                            .size(size)
                            .trackTotalHits(t -> t.enabled(true));
                    if (excludes != null) {
                        req.source(src -> src.filter(fetch -> fetch.excludes(excludes)));
                    }
                    return req;
                }
                , Map.class);
        return searchResponse.hits();
    }

    public Serializable getProperty(String nodeRef, String property) throws IOException {
        List<String> excludes = new ArrayList<>();
        excludes.add("preview");
        excludes.add("content");
        Map<String, Object> sourceMap = getSourceMap(nodeRef, excludes);
        return (sourceMap == null) ? null : (Serializable) sourceMap.get(property);
    }

    public Map<String, Object> getSourceMap(String nodeRef) throws IOException {
        return this.getSourceMap(nodeRef, null);
    }

    public Map<String, Object> getSourceMap(String nodeRef, List<String> excludes) throws IOException {

        String uuid = Tools.getUUID(nodeRef);
        String protocol = Tools.getProtocol(nodeRef);
        String identifier = Tools.getIdentifier(nodeRef);
        Query query = Query.of(q -> q.bool(b -> b
                .must(must -> must.term(t -> t.field("nodeRef.id").value(uuid)))
                .must(must -> must.term(t -> t.field("nodeRef.storeRef.protocol").value(protocol)))
                .must(must -> must.term(t -> t.field("nodeRef.storeRef.identifier").value(identifier)))));

        HitsMetadata<Map> sh = this.search(INDEX_WORKSPACE, query, 0, 1, excludes);
        if (sh == null || sh.total().value() == 0) {
            return null;
        }

        Hit<Map> searchHit = sh.hits().get(0);
        //noinspection unchecked
        return (Map<String, Object>) searchHit.source();
    }

    /**
     * @param nodeStatistics
     * @return true when all uuids already exist in index
     * @throws IOException
     */
    public boolean updateNodeStatistics(Map<String, List<NodeStatistic>> nodeStatistics) throws IOException {

        AtomicBoolean allInIndex = new AtomicBoolean();
        allInIndex.set(true);
        //boolean allInIndex = true;

        AtomicInteger counter = new AtomicInteger();
        int chunkSize = 100;

        try {
            nodeStatistics.entrySet().stream().collect(Collectors.groupingBy(e -> counter.getAndIncrement() / chunkSize)).forEach((groupKey, group) -> {

                logger.info("starting with page:" + groupKey + " collection size:" + group.size());
                try {
                    List<BulkOperation> bulk = new ArrayList<>();
                    for (Map.Entry<String, List<NodeStatistic>> entry : group) {
                        String uuid = entry.getKey();
                        List<NodeStatistic> statistics = entry.getValue();
                        if (statistics == null || statistics.isEmpty()) continue;

                        String nodeRef = CCConstants.STORE_WORKSPACES_SPACES + "/" + uuid;
                        Serializable value = this.getProperty(nodeRef, "dbid");
                        if (value == null) {
                            String nodeRefArchive = CCConstants.ARCHIVE_STOREREF + "/" + uuid;
                            value = this.getProperty(nodeRefArchive, "dbid");

                            if (value == null) {
                                logger.info("uuid:" + uuid + " is not in elastic in elastic index");
                                allInIndex.set(false);
                                continue;
                            }
                        }

                        Long dbid = ((Number) value).longValue();

                        DataBuilder builder = new DataBuilder();
                        Map<String, Integer> dayCountsView = new HashMap<>();
                        Map<String, Integer> dayCountsDownload = new HashMap<>();

                        builder.startObject();
                        for (NodeStatistic nodeStatistic : statistics) {
                            if (nodeStatistic == null) {
                                logger.debug("there is a null value in statistics list:" + nodeRef);
                                continue;
                            }
                            if (nodeStatistic.getCounts() == null || nodeStatistic.getCounts().size() == 0) continue;
                            String DOWNLOAD = "DOWNLOAD_MATERIAL";
                            String VIEW = "VIEW_MATERIAL";
                            String fieldNameDownload = "statistic_" + DOWNLOAD + "_" + nodeStatistic.getTimestamp();
                            String fieldNameView = "statistic_" + VIEW + "_" + nodeStatistic.getTimestamp();
                            Integer download = nodeStatistic.getCounts().get(DOWNLOAD);
                            Integer view = nodeStatistic.getCounts().get(VIEW);
                            if (download != null && download > 0) {
                                builder.field(fieldNameDownload, download);
                            }
                            if (view != null && view > 0) {
                                builder.field(fieldNameView, view);
                            }

                        }

                        builder.endObject();
                        bulk.add(BulkOperation.of(req -> req
                                .update(i -> i
                                        .index(INDEX_WORKSPACE)
                                        .id(Long.toString(dbid))
                                        .action(a -> a.doc(builder.build())))));
                    }
                    this.updateBulk(bulk);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            throw (IOException) e.getCause();
        }

        return allInIndex.get();
    }


    public void cleanUpNodeStatistics(List<String> nodeUuids) throws IOException {
        logger.info("starting cleanUpNodeStatistics");
        for (String uuid : nodeUuids) {
            cleanUpNodeStatistics(uuid);
        }
        logger.info("returning cleanUpNodeStatistics");
    }


    public void cleanUpNodeStatistics(String nodeUuid) throws IOException {

        List<String> excludes = new ArrayList<>();
        excludes.add("preview");
        excludes.add("content");
        Map<String, Object> sourceMap = getSourceMap("workspace://SpacesStore/" + nodeUuid, excludes);
        if (sourceMap == null) {
            return;
        }

        long dbid = ((Number) sourceMap.get("dbid")).longValue();
        String id = Long.toString(dbid);
        String type = (String) sourceMap.get("type");

        if ("ccm:io".equals(type)) {
            List<String> propsToRemove = new ArrayList<>();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -statisticHistoryInDays);
            for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                if (!entry.getKey().startsWith("statistic_") || entry.getKey().startsWith("statistic_RATING")) {
                    continue;
                }

                String prefixPattern = "statistic_[a-zA-Z_]*";
                String datePattern = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
                if (!entry.getKey().matches(prefixPattern + datePattern)) {
                    continue;
                }

                String[] split = Pattern.compile(prefixPattern).split(entry.getKey());
                try {
                    Date date = statisticDateFormatter.parse(split[1]);
                    if (cal.getTime().getTime() > date.getTime()) {
                        propsToRemove.add(entry.getKey());
                    }
                } catch (ParseException e) {
                    logger.error("can not get date in: " + entry.getKey());
                }
            }

            if (propsToRemove.isEmpty()) {
                return;
            }

            logger.info("remove for " + id + ": " + String.join(",", propsToRemove));

            this.update(req -> req
                            .index(INDEX_WORKSPACE)
                            .id(id)
                            .script(scr -> scr
                                    .inline(il -> il
                                            .lang("painless")
                                            .source("for(String prop : params.propsToRemove){ctx._source.remove(prop)}")
                                            .params("propsToRemove", JsonData.of(propsToRemove)))),
                    Map.class);
        }
    }

}
