package com.target.search;

public class PerformanceSearchResult {

    private int status;
    private long msUsed;

    PerformanceSearchResult(int status, long msUsed) {
        this.status = status;
        this.msUsed = msUsed;
    }

    PerformanceSearchResult(long msUsed) {
        this.msUsed = msUsed;
    }

    int getStatus() {
        return status;
    }

    long getMsUsed() {
        return msUsed;
    }

}
