package com.target.search;

import java.io.IOException;

public class Main {
    public static void main(String... args) {
        GeneratePhrases generatePhrases = new GeneratePhrases();
        generatePhrases.generateRandomPhrases();

        SearchPerformanceTest searchPerformanceTest = new SearchPerformanceTest();
        SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
        simpleDocumentSearch.setUp();
        searchPerformanceTest.executeSimpleSearch(simpleDocumentSearch);

        RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
        regexDocumentSearch.setup();
        searchPerformanceTest.executeRegexSearch(regexDocumentSearch);

        IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();
        try {
            indexedDocumentSearch.setup();
            //searchPerformanceTest.executeIndexedSearch(indexedDocumentSearch);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        try {
//            List<String> phrases = Files.readAllLines(Paths.get("src/main/resources/generate_phrases.txt"));
//            for (String phrase : phrases) {
//                System.out.println("******  phrase - " + phrase);
//                simpleDocumentSearch.getSearchResults(phrase);
//                regexDocumentSearch.getSearchResults(phrase);
//                indexedDocumentSearch.getSearchResults(phrase);
//            }
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}

