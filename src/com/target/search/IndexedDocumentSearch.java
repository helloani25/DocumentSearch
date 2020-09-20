package com.target.search;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IndexedDocumentSearch implements DocumentSearch {

    private RestHighLevelClient client;
    private Map<Path, String> fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);

    public void setup() throws IOException {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        setupElasticsearch();
        if (!checkIfIndexExists()) {
            createIndex();
            putIndexMapping();
        }
        indexFiles();
    }

    @Override
    public void getSearchResults(String phrase) {

        setupElasticsearch();
        SearchRequest searchRequest = buildSearchRequest(phrase);
        SearchResponse searchResponse;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            Set<String> fileSet;
            TimeValue took;
            if (searchResponse.status() == RestStatus.OK) {
                took = searchResponse.getTook();
                SearchHits searchHits = searchResponse.getHits();
                fileSet = getSuccessSearch(searchHits);
                printSuccessSearch(phrase, fileSet);
                System.out.println("Elapsed Time : " + took.toString());
            }
            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupElasticsearch()  {
         client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"))
                .setRequestConfigCallback(
                requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(60).setSocketTimeout(60)));
    }

    private void createIndex() throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("target");
        createIndexRequest.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
        );
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        if (!createIndexResponse.isAcknowledged())
            throw new RuntimeException("Index target was not craeted");
    }

    private void putIndexMapping() throws IOException {
        PutMappingRequest request = new PutMappingRequest("target");
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        content.put("type", "text");
        Map<String, Object> filename = new HashMap<>();
        filename.put("type", "keyword");
        //Don't have to aggregate
        filename.put("doc_values", false);
        Map<String, Object> properties = new HashMap<>();
        properties.put("content", content);
        properties.put("filename", filename);
        jsonMap.put("properties", properties);
        request.source(jsonMap);
        request.setTimeout(TimeValue.timeValueSeconds(15));
        request.setMasterTimeout(TimeValue.timeValueSeconds(15));
        AcknowledgedResponse putMappingResponse = client.indices().putMapping(request, RequestOptions.DEFAULT);
        if (!putMappingResponse.isAcknowledged())
            throw new RuntimeException("Mapping was not persisited for Document index target");
    }

    private boolean checkIfIndexExists() throws IOException {
        GetIndexRequest request = new GetIndexRequest("target");
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN_CLOSED);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    private void indexFiles() throws IOException {
        int id = 0;
        BulkRequest request = new BulkRequest();
        request.timeout(TimeValue.timeValueMinutes(2));
        request.waitForActiveShards(1);

        for (Path file : fileMap.keySet()) {
            id++;
            IndexRequest indexRequest = new IndexRequest("target")
                    .id(String.valueOf(id))
                    .source("filename", file.getFileName().toString(),
                            "content", fileMap.get(file));
            request.add(indexRequest);

        }
        BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
        if (bulkResponse.status() != RestStatus.OK)
            throw new RuntimeException("Indexing for target was not successful");

        if (bulkResponse.hasFailures()) {
            throw new RuntimeException(bulkResponse.buildFailureMessage());
        }
        for (BulkItemResponse baa : bulkResponse) {
            if (baa.getResponse().getShardInfo().getFailed() > 0) {
                throw new RuntimeException("Shard level failure");
            }
        }

    }

    private SearchRequest buildSearchRequest(String phrase) {
        SearchRequest searchRequest = new SearchRequest("target");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("content", phrase));
        searchRequest.source(searchSourceBuilder);
        String[] includeFields = new String[]{"filename"};
        String[] excludeFields = new String[]{"content"};
        searchSourceBuilder.fetchSource(includeFields, excludeFields);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        HighlightBuilder.Field highlightTitle =
                new HighlightBuilder.Field("content");
        highlightTitle.highlighterType("unified");
        highlightBuilder.field(highlightTitle);
        searchSourceBuilder.highlighter(highlightBuilder);

        return searchRequest;
    }

    private void printSuccessSearch(String phrase, Set<String> fileSet) {
        System.out.println("Search Results:");
        for (Path file : fileMap.keySet()) {
            if (fileSet.contains(file.getFileName().toString())) {
                System.out.println(file + " " + phrase + " matches");
            } else {
                System.out.println(file + " " + phrase + " no match");
            }
        }
    }

    private Set<String> getSuccessSearch(SearchHits searchHits) {
        Set<String> fileSet = new HashSet<>();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String filename = (String) sourceAsMap.get("filename");
            fileSet.add(filename);
        }
        return fileSet;
    }

}
