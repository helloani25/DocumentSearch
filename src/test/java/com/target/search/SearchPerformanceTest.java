package com.target.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SearchPerformanceTest {

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
            e.printStackTrace();
        }
        executor.shutdown();
    }

    public void computeTimePerRequests(List<Future<PerformanceSearchResult>> futures, int totalNumRequests, long preprocessTime, String searchType) throws ExecutionException, InterruptedException {
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

    public void printTestResults(List<Future<PerformanceSearchResult>> futures, int totalNumRequests, long preprocessTime, String searchType) {
        try {
            computeTimePerRequests(futures, totalNumRequests, preprocessTime, searchType);
        } catch (ExecutionException|InterruptedException e) {
            System.out.println("Unable to fetch results from submitted search requests");
            e.printStackTrace();
        }
    }


}
