package com.target.search;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IndexedDocumentSearch implements DocumentSearch {

    private Map<Path, String> fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
    private static Logger logger = LogManager.getLogger(IndexedDocumentSearch.class);
    private long timeElapased = 0;

    public void setup() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
    }

    @Override
    public void getSearchResults(String phrase) {
        logger.debug("Inside get search results");
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(60000)))) {
            // do something with the client
            // client gets closed automatically
            if (!checkIfIndexExists(client)) {
                createIndex(client);
                putIndexMapping(client);
            }
            indexFiles(client);
            executeSearchRequest(client, phrase);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void createIndex(RestHighLevelClient client) throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("target");
        createIndexRequest.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
        );
        long startTime = System.nanoTime();
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        long endTime = System.nanoTime();
        timeElapased += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        if (!createIndexResponse.isAcknowledged())
            throw new RuntimeException("Index target was not created");
    }

    private void putIndexMapping(RestHighLevelClient client) throws IOException {
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
        request.setTimeout(TimeValue.timeValueSeconds(1));
        request.setMasterTimeout(TimeValue.timeValueSeconds(1));
        long startTime = System.nanoTime();
        AcknowledgedResponse putMappingResponse = client.indices().putMapping(request, RequestOptions.DEFAULT);
        long endTime = System.nanoTime();
        timeElapased += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        if (!putMappingResponse.isAcknowledged())
            throw new RuntimeException("Mapping was not persisted for Document index target");
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
        request.waitForActiveShards(1);

        for (Path file : fileMap.keySet()) {
            id++;
            String filename = file.getFileName().toString();
            IndexRequest indexRequest = new IndexRequest("target")
                    .id(String.valueOf(id))
                    .source("filename", filename ,
                            "content", fileMap.get(file));
            request.add(indexRequest);

        }
        BulkResponse bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
        if (bulkResponse.getIngestTookInMillis() != -1)
            timeElapased += bulkResponse.getIngestTookInMillis();
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
        searchRequest.source(searchSourceBuilder);

        return searchRequest;
    }

    private void  executeSearchRequest(RestHighLevelClient client, String phrase) throws IOException {
        SearchRequest searchRequest = buildSearchRequest(phrase);
        SearchResponse searchResponse;

        searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        if (searchResponse.status() == RestStatus.OK) {
            long timeElapsed = searchResponse.getTook().millis() + timeElapased;
            SearchHits searchHits = searchResponse.getHits();
            Set<String> fileSet = getSuccessSearch(searchHits);
            printSuccessSearch(phrase, fileSet);
            System.out.println("Elapsed Time : " + timeElapsed+"ms");
        }
    }

    private void printSuccessSearch(String phrase, Set<String> fileSet) {
        System.out.println("Search Results:");
        for (Path file : fileMap.keySet()) {
            if (fileSet.contains(file.getFileName().toString())) {
                System.out.println(file.getFileName().toString() + " - matches");
            } else {
                System.out.println(file.getFileName().toString() + " - no match");
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
