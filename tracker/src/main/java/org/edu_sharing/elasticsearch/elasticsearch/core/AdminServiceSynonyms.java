package org.edu_sharing.elasticsearch.elasticsearch.core;

import co.elastic.clients.elasticsearch.synonyms.ElasticsearchSynonymsClient;
import co.elastic.clients.elasticsearch.synonyms.SynonymRule;
import co.elastic.clients.transport.ElasticsearchTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.elasticsearch.tracker.Partition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class AdminServiceSynonyms {

    private final ElasticsearchSynonymsClient synonymsClient;

    @PostConstruct
    private void init() throws IOException{
        putSynonymsSet();
    }

    public void putSynonymsSet() throws IOException {
        //InputStream is = getClass().getClassLoader().getResourceAsStream("synonyms.txt");

        Path pathSynFile = Paths.get("synonyms.txt");
        Path pathSynHashFile = Paths.get("synonymsHash.txt");

        if(!Files.exists(pathSynFile)){
            return;
        }

        String currentFileHash = getFileHash(pathSynFile);
        String lastFileHash = Files.exists(pathSynHashFile) ? Files.readString(pathSynHashFile) : null;

        if(currentFileHash.equals(lastFileHash)){
            return;
        }

        List<SynonymRule> rules = new ArrayList<>();
        try (Stream<String> stream = Files.lines(pathSynFile)) {
            stream.forEach(s -> rules.add(new SynonymRule.Builder().synonyms(s).build()));
        } catch (IOException e) {
            //@TODO
            e.printStackTrace();
        }

        /**
         * create an own synonymset for every 10000 entrie
         */
        Collection<List<SynonymRule>> partitions = Partition.getPartitions(rules, 10000);


        try {
            int suffixId = 1;
            for(List<SynonymRule> r : partitions){
                String suffix = (suffixId == 1) ? "" : "_"+suffixId;
                System.out.println("put "+ r.size() +" synonyms ");
                synonymsClient.putSynonym(syn -> syn
                        .id("es-synonym-set"+suffix)
                        .synonymsSet(r));
                suffixId++;
            }
            Files.writeString(pathSynHashFile,currentFileHash);
        }catch (co.elastic.clients.transport.TransportException e ){
            e.printStackTrace();
            Files.writeString(pathSynHashFile,currentFileHash);
            //known elastic bug deserializing response
            //https://github.com/elastic/elasticsearch-java/issues/784
        }
    }

    private String getFileHash(Path pathSynFileName){
        try {
            byte[] data = Files.readAllBytes(pathSynFileName);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return Base64.encodeBase64String(hash);
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}
