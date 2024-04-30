package com.example.demo.service;

import com.example.demo.config.EsConfig;
import com.example.demo.modal.TitleUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Service
@Slf4j
public class ImportDataService {
    private final EsConfig esConfig;
    private final RestHighLevelClient esClient;
    private final HashMap<String, Object> user;
    private final HashMap<String, Object> session;
    private final HashMap<String, Object> event;
    private final TitleUrl[] title_url;
    ObjectMapper objectMapper = new ObjectMapper();
    final String indexName = "leadboxer_test_index";
    final String doc = "doc";
    final int SESSION_1 = 1;
    final int SESSION_2 = 2;

    public ImportDataService(EsConfig esConfig,
                             @Qualifier("elasticClient") RestHighLevelClient esClient) throws IOException {
        this.esConfig = esConfig;
        this.esClient = esClient;

        File jsonFileForUser = new File("src/main/resources/static/user.json");
        user = objectMapper.readValue(jsonFileForUser, HashMap.class);

        File jsonFileForSession = new File("src/main/resources/static/session1.json");
        session = objectMapper.readValue(jsonFileForSession, HashMap.class);

        File jsonFileForEvent = new File("src/main/resources/static/event1.json");
        event = objectMapper.readValue(jsonFileForEvent, HashMap.class);

        File jsonFileForTitle_Url = new File("src/main/resources/static/title_url.json");
        title_url = objectMapper.readValue(jsonFileForTitle_Url, TitleUrl[].class);
    }

    public void checkEs() throws IOException {
        BoolQueryBuilder bqb = new BoolQueryBuilder();
        bqb.must(QueryBuilders.matchAllQuery());
        SearchRequest searchRequest = buildSearchRequest(bqb);
        SearchResponse search = esClient.search(searchRequest);
        int length = search.getHits().getHits().length;
        log.info("length: " + length);
    }

    private SearchRequest buildSearchRequest(BoolQueryBuilder boolQueryBuilder) {
        SearchRequest searchRequest = new SearchRequest(esConfig.getIndex());
        searchRequest.types("doc");
        SearchSourceBuilder sourceBuilder = buildSearchSource(boolQueryBuilder);
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

    private SearchSourceBuilder buildSearchSource(BoolQueryBuilder boolQueryBuilder) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        sourceBuilder.query(boolQueryBuilder);

        // Enable for debugging
        printQuery("buildSearchSource:", sourceBuilder);
        return sourceBuilder;
    }

    public void printQuery(String message, SearchSourceBuilder searchSourceBuilder) {
        HashMap<String, String> params = new HashMap<>();
        params.put("pretty", "true");
        log.info(message + " => " + searchSourceBuilder.toString(new ToXContent.MapParams(params)));
    }

    public void user(String formattedNumber, BulkRequest bulkRequest) throws IOException {
        HashMap<String, Object> hashMap = user;
        String user_id = "u_" + formattedNumber;
        hashMap.replace("use_id", formattedNumber);

        String jsonData = objectMapper.writeValueAsString(hashMap);
        bulkRequest.add(new IndexRequest(indexName)
                .id(user_id)
                .type(doc)
                .source(jsonData, XContentType.JSON));
        newSession(formattedNumber, SESSION_1, bulkRequest);
        newSession(formattedNumber, SESSION_2, bulkRequest);
    }

    public void newSession(String use_id, int s_id, BulkRequest bulkRequest) throws IOException {
        HashMap<String, Object> hashMap = session;
        String formattedNumber = String.format("%09d", s_id);
        String session_id = "s_" + formattedNumber + "_" + use_id;
        String user_id = "u_" + use_id;
        hashMap.replace("use_id", use_id);
        hashMap.replace("session_id", session_id);
        hashMap.replace("original_url", title_url[Integer.parseInt(use_id) % title_url.length].getImage());
        HashMap<String, String> type = new HashMap<>();
        type.put("parent", user_id);
        type.put("name", "session");
        hashMap.replace("type", type);

        String jsonData = objectMapper.writeValueAsString(hashMap);
        bulkRequest.add(new IndexRequest(indexName)
                .id(session_id)
                .type(doc)
                .routing(user_id)
                .source(jsonData, XContentType.JSON));
        newEvent(use_id, formattedNumber, bulkRequest);
    }

    public void newEvent(String use_id, String e_id, BulkRequest bulkRequest) throws IOException {
        HashMap<String, Object> hashMap = event;
        String user_id = "u_" + use_id;
        String event_id = "e_" + e_id + "_" + use_id;
        hashMap.replace("use_id", use_id);
        hashMap.replace("event_id", event_id);
        hashMap.replace("original_url", title_url[Integer.parseInt(use_id) % title_url.length].getImage());
        hashMap.replace("title", title_url[Integer.parseInt(use_id) % title_url.length].getTitle());

        HashMap<String, String> type = new HashMap<>();
        type.put("parent", "u_" + use_id);
        type.put("name", "event");
        hashMap.replace("type", type);

        String jsonData = objectMapper.writeValueAsString(hashMap);
        bulkRequest.add(new IndexRequest(indexName)
                .id(event_id)
                .type(doc)
                .routing(user_id)
                .source(jsonData, XContentType.JSON));
    }

    public void addDummyData() throws IOException {

        int totalData = 1000;
        int number = 1;
        BulkRequest bulkRequest = new BulkRequest();
        while (number <= totalData) {
            String formattedNumber = String.format("%09d", number++);
            user(formattedNumber, bulkRequest);
        }
        BulkResponse bulkResponse = esClient.bulk(bulkRequest);
        if (bulkResponse.hasFailures()) {
            System.out.println("Bulk request has failures: " + bulkResponse.buildFailureMessage());
        } else {
            System.out.println("Bulk request successful");
        }
    }
}
