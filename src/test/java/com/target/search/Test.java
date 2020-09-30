package com.target.search;

public class Test {
    public static void main(String... args) {
        GeneratePhrases generatePhrases = new GeneratePhrases();
        generatePhrases.generateRandomPhrases();

        SearchPerformanceTest searchPerformanceTest = new SearchPerformanceTest();
        DocumentSearch documentSearch;
        documentSearch = new SimpleDocumentSearch();
        documentSearch.setup();
        searchPerformanceTest.executeSearch(documentSearch, "Simple Match");


        documentSearch = new RegexDocumentSearch();
        documentSearch.setup();
        searchPerformanceTest.executeSearch(documentSearch, "Regex Match ");

        documentSearch = new IndexedDocumentSearch();
        documentSearch.setup();
        searchPerformanceTest.executeSearch(documentSearch, "Indexed Match");

    }
}

