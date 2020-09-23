package com.target.search;

import java.io.IOException;

public class Main {

    public static void main(String... args) {
        String phrase = "Following defeat in the Franco-Prussian War, Franco-German rivalry erupted again in the First World War. France and its allies were victorious this time. Social, political, and economic upheaval in the wake of the conflict led to the Second World War, in which the Allies were defeated in the Battle of France and the French government surrendered and was replaced with an authoritarian regime. The Allies, including the government in exile's Free French Forces and later a liberated French nation, eventually emerged victorious over the Axis powers. As a result, France secured an occupation zone in Germany and a permanent seat on the United Nations Security Council. The imperative of avoiding a third Franco-German conflict on the scale of those of two world wars paved the way for European integration starting in the 1950s. France became a nuclear power and since the 1990s its military action is most often seen in cooperation with NATO and its European partners.";
        SimpleDocumentSearch simpleDocumentSearch = new SimpleDocumentSearch();
        simpleDocumentSearch.setUp();
        simpleDocumentSearch.getSearchResults(phrase);
        RegexDocumentSearch regexDocumentSearch = new RegexDocumentSearch();
        regexDocumentSearch.setup();
        regexDocumentSearch.getSearchResults(phrase);
        IndexedDocumentSearch indexedDocumentSearch = new IndexedDocumentSearch();
        try {
            indexedDocumentSearch.setup();
            indexedDocumentSearch.getSearchResults(phrase);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


