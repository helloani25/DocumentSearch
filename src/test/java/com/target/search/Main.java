package com.target.search;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String... args) {
        GeneratePhrases generatePhrases = new GeneratePhrases();
        generatePhrases.generateRandomPhrases();
        SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
        simpleDocumentSearch.setUp();
        RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
        regexDocumentSearch.setup();
        IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();
        indexedDocumentSearch.setup();
        SearchPerformanceTest searchPerformanceTest = new SearchPerformanceTest();
        //searchPerformanceTest.executeSimpleSearch(simpleDocumentSearch);
        //searchPerformanceTest.executeRegexSearch(regexDocumentSearch);
        //searchPerformanceTest.executeIndexedSearch(indexedDocumentSearch);

        try {
            List<String> phrases = Files.readAllLines(Paths.get("src/main/resources/generate_phrases.txt"));
            for (String phrase : phrases) {
                System.out.println("******  phrase - " + phrase);
                //simpleDocumentSearch.getSearchResults(phrase);
                //regexDocumentSearch.getSearchResults(phrase);
                indexedDocumentSearch.getSearchResults(phrase);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

