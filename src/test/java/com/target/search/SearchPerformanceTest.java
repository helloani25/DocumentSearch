package com.target.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Performance tests for 2M searches. Uses fixed threadpool executor with 4 threads to call
 * the search function concurrently
 */
public class SearchPerformanceTest {

    private final static Logger logger = LogManager.getLogger(SearchPerformanceTest.class);

    /**
     * Execute all three search methods and check the performance of each of them
     * @param documentSearch  document search type to be executed
     * @param searchType Simpe Search, Regular expression and Indexed
     */
    public void executeSearch(DocumentSearch documentSearch, String searchType) {
        ExecutorService executor = Executors.newFixedThreadPool(Constants.CONCURRENCY_LEVEL);
        List<Future<PerformanceSearchResult>> futures = new ArrayList<>();
        int totalNumRequests = 0;
        try {
            List<String> phrases = Files.readAllLines(Paths.get(Constants.PHRASE_FIILE));
            for (String phrase : phrases) {
                totalNumRequests++;
                Callable<PerformanceSearchResult> task = () -> documentSearch.getSearchResults(phrase);
                futures.add(executor.submit(task));
            }
            printTestResults(futures, totalNumRequests, documentSearch.getPreprocessTimeElapsed(), searchType);
        } catch (IOException e) {
            logger.error(e);
        }
        executor.shutdown();
    }

    /**
     * Compute the mean aveage time to execute 2M search rewquests
     * @param futures futures of all search request execution
     * @param totalNumRequests Total number of requests
     * @param preprocessTime Time taken to tokenize or create indexes for the documents
     * @param searchType Simple Search, Regular Expression or Indexed.
     * @throws ExecutionException Exception thrown if the callable task to search failed
     * @throws InterruptedException Exception thrown if the callable task to search failed
     */
    private void computeTimePerRequests(List<Future<PerformanceSearchResult>> futures, int totalNumRequests, long preprocessTime, String searchType) throws ExecutionException, InterruptedException {
        double sum = 0;
        int success = 0;
        int failures= 0;
        for (Future<PerformanceSearchResult> future: futures) {
            PerformanceSearchResult response = future.get();
            if (response.getStatus() !=0 && response.getStatus() != 200)
                failures++;
            else
                success++;
            sum+= response.getMsUsed();
        }
        System.out.println(searchType.toUpperCase());
        System.out.println("Preprocessing Time/Tokenization: " + preprocessTime + " ms");
        System.out.println("Concurrency Level:               " + Constants.CONCURRENCY_LEVEL);
        System.out.println("Total number of requests :       " + totalNumRequests);
        System.out.println("Completed requests :             " + success);
        System.out.println("Failed requests :                " + failures);
        double average = sum/totalNumRequests;
        System.out.println("Time per request(mean) :         " + average + " ms");
    }

    private void printTestResults(List<Future<PerformanceSearchResult>> futures, int totalNumRequests, long preprocessTime, String searchType) {
        try {
            computeTimePerRequests(futures, totalNumRequests, preprocessTime, searchType);
        } catch (ExecutionException|InterruptedException e) {
            logger.info("Unable to fetch results from submitted search requests");
            logger.error(e);
        }
    }


}
