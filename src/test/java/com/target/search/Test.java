package com.target.search;

public class Test {
    public static void main(String... args) {
        GeneratePhrases generatePhrases = new GeneratePhrases();
        generatePhrases.generateRandomPhrases();

        SearchPerformanceTest searchPerformanceTest = new SearchPerformanceTest();
        DocumentSearch documentSearch;
        if (args.length == 0)
            System.out.println("Invalid Entry");
        else
            switch (args[0]) {
                case "1":
                case "String Match":
                    documentSearch = new SimpleDocumentSearch();
                    documentSearch.setup();
                    searchPerformanceTest.executeSearch(documentSearch, "Simple Match");
                    break;
                case "2":
                case "Regular Expression":
                    documentSearch = new RegexDocumentSearch();
                    documentSearch.setup();
                    searchPerformanceTest.executeSearch(documentSearch, "Regex Match");
                    break;
                case "3":
                case "Indexed":
                    documentSearch = new IndexedDocumentSearch();
                    documentSearch.setup();
                    searchPerformanceTest.executeSearch(documentSearch, "Indexed Match");
                    break;
                default:
                    System.out.println("Invalid Entry");
                    break;
            }


    }
}

