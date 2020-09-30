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

/**
 * Indexed search uses Elasticsearch to create an index named 'target' and all the files are written
 * to the content field. filename field is added a well. Using match phrase to search a keyword or a phrase
 */
public class IndexedDocumentSearch implements DocumentSearch {

    private Map<String, String> fileMap;
    private final static Logger logger = LogManager.getLogger(IndexedDocumentSearch.class);
    private long timeElapsed = 0;

    /**
     * Read all the files from the sample_text.txt and is placed in the fileMap.
     * Index target is created and mappings are created for the fields filename and content
     * Files are indexed in bulk and refreshed to create the lucene segments by refreshing the
     * index
     */
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

    /**
     * The total time taken to create the index target and it's mappings, ingest the documents
     * and refresh the index
     * @return Returns the total time taken for creating the index, bulk ingestion and refreshing the index
     */
    @Override
    public long getPreprocessTimeElapsed() {
        return timeElapsed;
    }

    /**
     * Search the phrase or keyword/term in the index target using Elasticsearch
     * @param phrase
     * @return Returns if the search was successful and the time elapsed
     */
    @Override
    public PerformanceSearchResult getSearchResults(String phrase) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http"))
                .setRequestConfigCallback(
                        requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(360000).setConnectionRequestTimeout(0)))) {
            return executeSearchRequest(client, phrase);

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Create the index target and add the mappings
     * @param client Restclient used to connect to the Elasticsearch cluster. In our case only one node
     * @throws IOException Throws an exception if the index cannot be created
     */
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

    /**
     * Create the mappings for the fields filename and the text. The filename need not be analyzed.
     * So its keyword type. The content of the file has to be analyzed. So the type is text
     * Disable doc_values for the keyword since we don' need aggregation. Text type do not have
     * aggregation by default.
     * @return mapping object to used in creating the index
     */
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

    /**
     * Check if the index already exists before trying to index the files
     * @param client Restclient to connect to the Elasticsearch cluster
     * @return true of false to notify if the index was created
     * @throws IOException Exception if the HTTP call to check index exists fails
     */
    private boolean checkIfIndexExists(RestHighLevelClient client) throws IOException {
        GetIndexRequest request = new GetIndexRequest("target");
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);
        request.indicesOptions(IndicesOptions.STRICT_EXPAND_OPEN_CLOSED);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * Index all the files in the sample_text.txt
     * @param client Restclient used to connect to the elasticsearch cluster
     * @throws IOException Exception thrown if the indexing fails
     */
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

    /**
     * Refresh the index to make it avaialable for search. This will create the lucene segements
     * if it does not exist in memory
     * @param client Restclient to connect to the elasticsearch cluster/single node in our case
     */
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

    /**
     * Build the search query for elasticsearch
     * @param phrase Match with the term or the phrase
     * @return Returns search request object
     */
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
        long timeUsed;
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
