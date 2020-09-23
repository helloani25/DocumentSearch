package com.target.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchPerformanceTest {

    public void executeSimpleSearch(SimpleDocumentSearch simpleDocumentSearch) {

        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<String> phrases = Files.readAllLines(Paths.get("resources/generate_phrases.txt"));
            for (String phrase : phrases) {
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        simpleDocumentSearch.getSearchResults(phrase);
                    }
                };
                executor.execute(task);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }

    public void executeRegexSearch(RegexDocumentSearch regexDocumentSearch) {

        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<String> phrases = Files.readAllLines(Paths.get("resources/generate_phrases.txt"));
            for (String phrase : phrases) {
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        regexDocumentSearch.getSearchResults(phrase);
                    }
                };
                executor.execute(task);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }

    public void executeIndexedSearch(IndexedDocumentSearch indexedDocumentSearch) {

        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<String> phrases = Files.readAllLines(Paths.get("resources/generate_phrases.txt"));
            for (String phrase : phrases) {
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        indexedDocumentSearch.getSearchResults(phrase);
                    }
                };
                executor.execute(task);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdown();
    }




}
