package com.target.search;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IndexedDocumentSearch implements DocumentSearch {

    private Map<String, String> fileMap;
    private final static Logger logger = LogManager.getLogger(IndexedDocumentSearch.class);
    private long timeElapsed = 0;

    @Override
    public void setup() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000).setConnectionRequestTimeout(0)))) {
            // do something with the client
            // client gets closed automatically
            if (!checkIfIndexExists(client)) {
                createIndex(client);
                indexFiles(client);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PerformanceSearchResult getSearchResults(String phrase) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(360000).setConnectionRequestTimeout(0)))) {
            PerformanceSearchResult performanceSearchResult = executeSearchRequest(client, phrase);
            return  performanceSearchResult;

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private void createIndex(RestHighLevelClient client) throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("target");
        createIndexRequest.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put("index.requests.cache.enable",true)
        );
        createIndexRequest.waitForActiveShards();
        createIndexRequest.mapping(getIndexMapping());
        long startTime = System.nanoTime();
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        timeElapsed += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        if (!createIndexResponse.isAcknowledged())
            throw new RuntimeException("Index target was not created");
    }

    private Map<String, Object> getIndexMapping() {
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
        return jsonMap;
    }

    private boolean checkIfIndexExists(RestHighLevelClient client) throws IOException {
        GetIndexRequest request = new GetIndexRequest("target");
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN_CLOSED);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    private void indexFiles(RestHighLevelClient client) throws IOException {
        int id = 0;
        BulkRequest request = new BulkRequest();
        request.timeout(TimeValue.timeValueMinutes(2));
        request.waitForActiveShards();
        for (String filename : fileMap.keySet()) {
            id++;

            IndexRequest indexRequest = new IndexRequest("target")
                    .id(String.valueOf(id))
                    .source("filename", filename ,
                            "content", fileMap.get(filename));
            request.add(indexRequest);

        }
        BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
        if (bulkResponse.getIngestTookInMillis() != -1)
            timeElapsed += bulkResponse.getIngestTookInMillis();
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
        refreshBeforeSearch(client);
    }

    private void refreshBeforeSearch(RestHighLevelClient client) {
        try {
            long startTime = System.nanoTime();
            RefreshRequest refreshRequest = new RefreshRequest("target");
            client.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
            timeElapsed += TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        } catch (ElasticsearchException|IOException exception) {
                throw new RuntimeException("Refreshing before search for target was not successful");
        }
    }

    private SearchRequest buildSearchRequest(String phrase) {
        SearchRequest searchRequest = new SearchRequest("target");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("content", phrase));
        String[] includeFields = new String[]{"filename"};
        String[] excludeFields = new String[]{"content"};
        searchSourceBuilder.fetchSource(includeFields, excludeFields);
        searchSourceBuilder.timeout(new TimeValue(30, TimeUnit.SECONDS));
        searchRequest.source(searchSourceBuilder);

        return searchRequest;
    }

    private PerformanceSearchResult executeSearchRequest(RestHighLevelClient client, String phrase) {
        SearchRequest searchRequest = buildSearchRequest(phrase);
        SearchResponse searchResponse;
        long timeUsed = 0;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            timeUsed = searchResponse.getTook().getMillis();
            if (searchResponse.status() == RestStatus.OK) {
                SearchHits searchHits = searchResponse.getHits();
                StringBuilder sb = new StringBuilder();
                Set<String> fileSet = printMatchSearch(searchHits, sb);
                printNoMatchSearch(fileSet, sb, timeUsed);
            }
            return new PerformanceSearchResult(searchResponse.status().getStatus(), timeUsed);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void printNoMatchSearch(Set<String> fileSet, StringBuilder sb, long timeUsed) {
        for (String filename : fileMap.keySet()) {
            if (!fileSet.contains(filename)) {
                sb.append(filename).append(" ").append(" - no match\n");
            }
        }
        sb.append("Elapsed Time : ").append(timeUsed).append("ms\n");
        System.out.println(sb.toString());
    }

    private Set<String> printMatchSearch(SearchHits searchHits, StringBuilder sb) {
        sb.append("Search Results:\n");
        Set<String> fileSet = new HashSet<>();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String filename = (String) sourceAsMap.get("filename");
            fileSet.add(filename);
            sb.append(filename).append(" ").append(" - matches\n");
        }
        return fileSet;
    }

}
