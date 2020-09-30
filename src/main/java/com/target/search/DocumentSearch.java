package com.target.search;

/**
 * Document Search interface supports 3 different search types
 * Simple, regex and indexed
 */
public interface DocumentSearch {
    PerformanceSearchResult getSearchResults(String phrase);
    void setup();
    long getPreprocessTimeElapsed();
}
