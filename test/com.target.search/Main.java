package com.target.search;

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
        searchPerformanceTest.executeSimpleSearch(simpleDocumentSearch);
        searchPerformanceTest.executeRegexSearch(regexDocumentSearch);
        searchPerformanceTest.executeIndexedSearch(indexedDocumentSearch);
    }
}

