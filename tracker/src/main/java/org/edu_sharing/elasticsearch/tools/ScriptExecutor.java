package org.edu_sharing.elasticsearch.tools;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.sourceforge.cardme.engine.VCardEngine;
import net.sourceforge.cardme.vcard.VCard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.edu_sharing.elasticsearch.alfresco.client.NodeData;
import org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingClient;
import org.edu_sharing.elasticsearch.elasticsearch.client.ElasticsearchClient;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * this class checks for groovy scripts in the "scripts" resources folder and executes them
 * The return values of each script (must be a map) will be stored in the "customProperties" section of the index
 * If multiple scripts are present, they will be sorted by filename and run after each other
 * Each script will get information about the current node
 * Check the {@link #getBindings(Map)} method for a list of available attributes
 */
@Service
public class ScriptExecutor {
    static Logger logger = LogManager.getLogger(ScriptExecutor.class);
    @Autowired
    private EduSharingClient eduSharingClient;

    private final ScriptLoaderConfiguration.ScriptLoaderService scriptLoaderService;
    private File[] scripts = new File[0];

    public ScriptExecutor(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            ScriptLoaderConfiguration.ScriptLoaderService scriptLoaderService
    ) {
        this.scriptLoaderService = scriptLoaderService;
        init();
    }

    public boolean addCustomPropertiesByScript(XContentBuilder builder, NodeData nodeData) throws IOException {
        boolean hasData = false;
        Map<String, Serializable> metadata = nodeData.getNodeMetadata().getProperties().entrySet().stream()
                .collect(HashMap::new, (m,v)->m.put(CCConstants.getValidLocalName(v.getKey()), v.getValue()), HashMap::putAll);
        builder.startObject("customProperties");
        for (File script : scripts) {
            try {

                Binding sharedData = getBindings(metadata);
                GroovyShell shell = new GroovyShell(sharedData);
                Map<String, Serializable> result =
                        (Map<String, Serializable>) shell.evaluate(script);
                if(result != null) {
                    String mds = eduSharingClient.getMdsId(nodeData);
                    for (Map.Entry<String, Serializable> entry : result.entrySet()) {
                        String key = entry.getKey();
                        Serializable value = entry.getValue();
                        builder.field(key, value);
                        logger.debug("Script: " + script.getName() + ", key: " + key + ", value: " + value);
                        eduSharingClient.translateProperty(nodeData, mds, null, new AbstractMap.SimpleEntry<>(
                                "customProperties." + entry.getKey(), entry.getValue()
                        ));
                    }

                    hasData = true;
                }
            } catch(Throwable t) {
                logger.warn("Could not execute script " + script.getName(), t);
            }
        }
        builder.endObject();
        return hasData;
    }

    private Binding getBindings(Map<String, Serializable> metadata) {
        Binding sharedData = new Binding();
        sharedData.setProperty("metadata", metadata);
        sharedData.setProperty("contributor", getContributor(metadata));
        return sharedData;
    }

    private Map<String, Set<VCard>> getContributor(Map<String, Serializable> metadata) {
        Map<String, Set<VCard>> result = new HashMap<>();
        Set<String> contributorProperties = metadata.keySet().stream().filter(key -> key.matches(ElasticsearchClient.CONTRIBUTOR_REGEX)).collect(Collectors.toSet());
        if(contributorProperties.size() > 0){
            VCardEngine vcardEngine = new VCardEngine();
            contributorProperties.forEach(key -> {
                Serializable value = metadata.get(key);
                if (value instanceof List) {
                    List<String> mapped = (List<String>) value;
                    result.put(key, mapped.stream().filter(Objects::nonNull).map(v -> {
                        try {
                            return vcardEngine.parse(v);
                        } catch (Exception e) {
                            logger.debug(e);
                            return null;
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toSet()));
                }
            });
        }
        return result;
    }
    private void init() {
        try {
            scripts = scriptLoaderService.getFiles();
            if(scripts == null) {
                scripts = new File[0];
            }
            Arrays.sort(scripts);
            for (File script : scripts) {
                logger.info("Registered script: " + script.getName());
            }
        } catch (Throwable t) {
            logger.warn("Could not init scripts", t);
        }
    }
}
