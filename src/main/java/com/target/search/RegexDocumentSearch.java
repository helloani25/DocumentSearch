package com.target.search;


import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches using the regular expression for the phrase in all the files
 */
public class RegexDocumentSearch implements DocumentSearch {
    Map<String, String> fileMap;

    /**
     * Reads the files from the sample_text.txt and writes to a map
     */
    @Override
    public void setup() {
        fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
    }

    /**
     * No time is spent in tokenization. So its 0ms
     * @return
     */
    @Override
    public long getPreprocessTimeElapsed() {
        return 0;
    }

    /**
     * Searches for the term or the phrase using regular expression in all the files. and
     * orders the search results based on occurrence and frequency
     * @param phrase phrase or term to be matched
     * @return Total time taken
     */
    @Override
    public PerformanceSearchResult getSearchResults(String phrase) {
        Map<Integer, List<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        long startTime = System.nanoTime();
        for (String filename:fileMap.keySet()) {
            int count = findMatch(fileMap.get(filename), phrase.trim());
            List<String> list = treeMap.getOrDefault(count, new ArrayList<>());
            list.add(filename);
            treeMap.put(count, list);
        }
        long msUsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        printSearchResults(treeMap, msUsed);
        return new PerformanceSearchResult(msUsed);
    }

    private int findMatch(String content, String phrase) {
        phrase = phrase.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|:|\\?|\\-|;)","");
        content = content.replaceAll("\\[\\d+\\]", "");
        //Match with special characters removed
        content = content.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|:|\\?|\\-|;)","" );
        int count = 0;
        Pattern pattern = Pattern.compile("\\b("+phrase+")\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while(matcher.find()) {
            count++;
        }
        return count;
    }

    private void printSearchResults(Map<Integer, List<String>> treeMap, double msUsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search Results:\n");
        for (int count: treeMap.keySet())
        if (count == 0) {
            for (String filename: treeMap.get(count))
                sb.append(filename).append(" ").append(" - no match\n");
        } else {
            for (String filename: treeMap.get(count))
                sb.append(filename).append(" ").append(" - matches\n");
        }
        sb.append("Elapsed Time : ").append(msUsed).append("ms\n");
        System.out.println(sb.toString());
    }

}
