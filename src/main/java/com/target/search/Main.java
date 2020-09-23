package com.target.search;

public class Main {

    public static void main(String... args) {
        String phrase = "The Gallo-Roman conflict predominated from 60 BC to 50 BC";
        SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
        simpleDocumentSearch.setUp();
        simpleDocumentSearch.getSearchResults(phrase);
        RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
        regexDocumentSearch.setup();
        regexDocumentSearch.getSearchResults(phrase);
        IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();
        indexedDocumentSearch.setup();
        indexedDocumentSearch.getSearchResults(phrase);
    }
}
