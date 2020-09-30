package com.target.search;

public interface DocumentSearch {
    PerformanceSearchResult getSearchResults(String phrase);
    void setup();
}
