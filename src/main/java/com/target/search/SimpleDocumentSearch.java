package com.target.search;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Searches in all the files by preprocessing the text in words and saving it to an array for each file
 * Faster than regex because we don't match character by character.
 */
public class SimpleDocumentSearch implements DocumentSearch {
    private final Map<String, String[]> fileMapTokenzied;
    public SimpleDocumentSearch() {
        fileMapTokenzied = new HashMap<>();
    }
    private long timeElapsed;

    /**
     * Get all the files and the contents in a map and then tokenize all the words and place in an array
     * The fileMapTokenized holds the filename as key and the array words in sequence as values
     */
    @Override
    public void setup() {
        Map<String, String> fileMap = DocumentSearchUtils.readDirectory(DocumentSearchConstants.DOCUMENT_SEARCH_DIRECTORY);
        long startTime = System.nanoTime();
        tokenizeText(fileMap);
        timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    /**
     * Total time taken to break down the sentences into words
     * @return Time taken to tokenize the words
     */
    @Override
    public long getPreprocessTimeElapsed() {
        return timeElapsed;
    }

    /**
     * Get the total time token for the search and order the search based on occurrence and frequency
     * @param phrase term or phrase to be searched
     * @return Time taken to search
     */
    @Override
    public PerformanceSearchResult getSearchResults(String phrase) {
        long startTime = System.nanoTime();
        TreeMap<Integer, List<String>> treeMap = new TreeMap<>(Collections.reverseOrder());
        for (String filename: fileMapTokenzied.keySet()) {
            int count = findMatch(fileMapTokenzied.get(filename), phrase.trim());
            List<String> list = treeMap.getOrDefault(count, new ArrayList<>());
            list.add(filename);
            treeMap.put(count, list);
        }
        long msUsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        printSearchResults(treeMap, msUsed);
        return new PerformanceSearchResult(msUsed);
    }

    private int findMatch(String[] content, String phrase) {
        phrase = phrase.replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?|;)","" );
        String[] tokens = phrase.split("(\\s+|\\-)");
        int j = 0, count = 0;
        for (int i = 0; i < content.length-tokens.length+1; i++) {
            while ( j < tokens.length && (content[i].compareTo(tokens[j]) == 0 || content[i].compareToIgnoreCase(tokens[j]) == 0)) {
                i++;
                j++;
            }
            if (j == tokens.length) {
                count++;
            }
            j = 0;
        }
        return count;
    }

    private void tokenizeText(Map<String, String> fileMap) {
        for (String filename: fileMap.keySet()) {
            String[] words = fileMap.get(filename).split("(\\s+|\\-)");
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].strip();
                words[i] = words[i].replaceAll("\\[\\d+\\]", "");
                words[i] = words[i].replaceAll("(\"|!|\\[|\\]|\\(|\\)|\\,|\\.|\\:|\\?|;)","" );
            }
            fileMapTokenzied.put(filename, words);
        }
    }

    private void printSearchResults(Map<Integer, List<String>> treeMap, long msUsed) {
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
