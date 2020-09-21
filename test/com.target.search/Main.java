package com.target.search;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String... args) {
        GeneratePhrases generatePhrases = new GeneratePhrases();
        generatePhrases.generateRandomPhrases();

        try {
            List<String> phrases = Files.readAllLines(Paths.get("resources/generate_phrases.txt"));
            for (String phrase : phrases) {
                System.out.println("******  phrase - " + phrase);
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
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

